/*
 * This file is part of Ultimate Spleef, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2016 FlibioStudio
 * Copyright (c) Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package io.github.flibiostudio.ultimatespleef.arena;

import io.github.flibiostudio.ultimatespleef.UltimateSpleef;

import io.github.flibio.minigamecore.arena.Arena;
import io.github.flibio.minigamecore.arena.ArenaData;
import io.github.flibio.minigamecore.arena.ArenaState;
import io.github.flibio.minigamecore.arena.ArenaStates;
import io.github.flibio.utils.message.MessageStorage;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class UArena extends Arena {

    private MessageStorage messages = UltimateSpleef.getMessageStorage();

    private CountdownRunnable lobbyCountdown = new CountdownRunnable(this);
    private GameCountdownRunnable gameCountdown = new GameCountdownRunnable(this);
    private GamePlayingRunnable gamePlaying = new GamePlayingRunnable(this);
    private GameOverRunnable gameOver = new GameOverRunnable(this);

    private int minPlayers = 1;
    private int maxPlayers = 8;

    private List<UUID> alivePlayers = new ArrayList<>();
    private boolean lobbyWaiting = true;
    private int lobbyTimer = 15;

    private Location<World> lobbySpawn;
    private int circleRad;
    private Location<World> circleCenter;
    private ArrayList<Location<World>> blocks = new ArrayList<Location<World>>();
    private boolean dedicated;

    protected UArena(String arenaName, ArenaData data) {
        super(arenaName, Sponge.getGame(), UltimateSpleef.access);
        overrideData(data);

        getData().addPreventHungerLoss(ArenaStates.ALL);
        getData().addPreventPlayerDamage(ArenaStates.ALL);
        getData().addPreventBlockModify(ArenaStates.ALL);

        lobbySpawn = getData().getLocation("lobby").get();
        circleCenter = getData().getLocation("circlecenter").get();
        dedicated = data.getVariable("dedicated", Boolean.class).get();
        circleRad = (int) getData().getLocation("circleedge").get().getPosition().distance(circleCenter.getPosition());
        blocks = getCircle(circleCenter, circleRad);

        getData().setTriggerPlayerEvents(dedicated);

        addArenaStateRunnable(ArenaStates.LOBBY_COUNTDOWN, lobbyCountdown);
        addArenaStateRunnable(ArenaStates.GAME_COUNTDOWN, gameCountdown);
        addArenaStateRunnable(ArenaStates.GAME_PLAYING, gamePlaying);
        addArenaStateRunnable(ArenaStates.GAME_OVER, gameOver);
        resetArena();

        Sponge.getScheduler().createTaskBuilder().execute(r -> {
            if (lobbyWaiting) {
                if (lobbyTimer == 0) {
                    lobbyWaiting = false;
                    if (onlinePlayers.size() >= minPlayers) {
                        arenaStateChange(ArenaStates.LOBBY_COUNTDOWN);
                    }
                }
                lobbyTimer--;
            } else {
                lobbyTimer = 15;
            }
        }).async().interval(1, TimeUnit.SECONDS).submit(UltimateSpleef.access);

        // Temporary fix until ChangeDataHolderEvent is implemented
        Sponge.getScheduler().createTaskBuilder().execute(r -> {
            onlinePlayers.forEach(uuid -> {
                Optional<Player> pOpt = resolvePlayer(uuid);
                if (pOpt.isPresent()) {
                    Sponge.getScheduler().createTaskBuilder().execute(c -> {
                        pOpt.get().offer(Keys.FOOD_LEVEL, 20);
                    }).submit(UltimateSpleef.access);
                }
            });
        }).async().interval(5, TimeUnit.SECONDS).submit(UltimateSpleef.access);

    }

    public static Optional<UArena> createArena(String arenaName, ArenaData data) {
        if (isDataPresent(data)) {
            return Optional.of(new UArena(arenaName, data));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void addOnlinePlayer(Player player) {
        if (getOnlinePlayers().size() >= maxPlayers) {
            // The arena is already full
            failed(player, "arena.full");
        } else {
            ArenaState currentState = getCurrentState();
            if (currentState.equals(ArenaStates.LOBBY_COUNTDOWN) || currentState.equals(ArenaStates.LOBBY_WAITING)) {
                // Allow the player to join
                resetPlayer(player);
                broadcast(messages.getMessage("arena.join", "player", player.getName()));
                onlinePlayers.add(player.getUniqueId());
                // Check if the game can be started
                if (!lobbyWaiting) {
                    if (onlinePlayers.size() >= minPlayers) {
                        // Start the lobby countdown
                        arenaStateChange(ArenaStates.LOBBY_COUNTDOWN);
                    }
                }
            } else {
                // The game is already in progress
                failed(player, "arena.inprogress");
            }
        }
    }

    @Override
    public void removeOnlinePlayer(Player player) {
        onlinePlayers.remove(player.getUniqueId());
        broadcast(messages.getMessage("arena.exit", "player", player.getName()));
        ArenaState currentState = getCurrentState();
        if (currentState.equals(ArenaStates.LOBBY_COUNTDOWN) && onlinePlayers.size() < minPlayers) {
            // There are not enough players to continue the lobby countdown
            lobbyCountdown.cancelCountdown();
            arenaStateChange(ArenaStates.LOBBY_WAITING);
            broadcast(messages.getMessage("arena.lobbycountstop"));
        } else if (currentState.equals(ArenaStates.GAME_PLAYING) && onlinePlayers.size() < minPlayers) {
            // There are not enough players to continue the game
            arenaStateChange(ArenaStates.LOBBY_WAITING);
            broadcast(messages.getMessage("arena.notenoughplayers"));
            for (Player p : resolvePlayers(onlinePlayers)) {
                resetPlayer(p);
            }
            resetArena();
        } else if (currentState.equals(ArenaStates.GAME_COUNTDOWN) && onlinePlayers.size() < minPlayers) {
            // There are not enough players to continue the game
            gameCountdown.cancelCountdown();
            arenaStateChange(ArenaStates.LOBBY_WAITING);
            broadcast(messages.getMessage("arena.gamecountstop"));
            for (Player p : resolvePlayers(onlinePlayers)) {
                resetPlayer(p);
            }
            resetArena();
        }
    }

    public List<Location<World>> getBlocks() {
        return blocks;
    }

    public Location<World> getCenter() {
        return circleCenter;
    }

    public void removeAlive(UUID uuid) {
        alivePlayers.remove(uuid);
    }

    public List<UUID> getAlive() {
        return alivePlayers;
    }

    public void resetOnlinePlayers() {
        onlinePlayers.clear();
        alivePlayers.clear();
    }

    private ArrayList<Location<World>> getCircle(Location<World> center, int r) {
        ArrayList<Location<World>> locations = new ArrayList<Location<World>>();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        World w = center.getExtent();
        int rSquared = r * r;
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                if ((cx - x) * (cx - x) + (cz - z) * (cz - z) <= rSquared) {
                    locations.add(new Location<World>(w, x, cy, z));
                }
            }
        }
        return locations;
    }

    public void resetArena() {
        blocks.forEach(block -> {
            block.setBlockType(BlockTypes.QUARTZ_BLOCK);
            block.sub(0, 1, 0).setBlockType(BlockTypes.AIR);
            block.sub(0, 2, 0).setBlockType(BlockTypes.AIR);
            block.sub(0, 3, 0).setBlockType(BlockTypes.AIR);
            block.sub(0, 4, 0).setBlockType(BlockTypes.AIR);
            block.sub(0, 5, 0).setBlockType(BlockTypes.AIR);
            block.getExtent().getEntities().forEach(entity -> {
                if (entity.getLocation().getPosition().distance(block.getPosition()) < 1.5) {
                    entity.remove();
                }
            });
        });
        alivePlayers.addAll(onlinePlayers);
        lobbyWaiting = true;
        lobbyTimer = 15;
    }

    private void resetPlayer(Player player) {
        player.setLocationSafely(lobbySpawn.add(0, 1, 0));
        player.offer(Keys.GAME_MODE, GameModes.SURVIVAL);
        player.offer(Keys.HEALTH, 20.0);
        player.offer(Keys.FOOD_LEVEL, 20);
    }

    private void failed(Player player, String messageKey) {
        if (dedicated) {
            player.kick(messages.getMessage(messageKey));
        } else {
            player.sendMessage(messages.getMessage(messageKey));
        }
    }

    public static boolean isDataPresent(ArenaData data) {
        if (data.getLocation("lobby").isPresent() && data.getLocation("circlecenter").isPresent() && data.getLocation("circleedge").isPresent() &&
                data.getVariable("dedicated", Boolean.class).isPresent()) {
            return !data.getVariable("dedicated", Boolean.class).get() ? data.getLocation("joinsign").isPresent() : true;
        } else {
            return false;
        }
    }

}
