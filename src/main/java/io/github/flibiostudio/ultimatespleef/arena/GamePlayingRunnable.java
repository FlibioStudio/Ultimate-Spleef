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

import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.cause.Cause;
import io.github.flibio.minigamecore.arena.ArenaStates;
import io.github.flibio.minigamecore.economy.EconomyManager;
import io.github.flibio.utils.message.MessageStorage;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class GamePlayingRunnable implements Runnable {

    private MessageStorage messages = UltimateSpleef.getMessageStorage();
    private EconomyManager economyManager = UltimateSpleef.access.minigame.getEconomyManager();

    private UArena arena;

    private List<UUID> cooldown = new ArrayList<>();

    public GamePlayingRunnable(UArena arena) {
        this.arena = arena;
        Sponge.getEventManager().registerListeners(UltimateSpleef.access, this);
    }

    public void run() {

    }

    @Listener
    public void onMove(DisplaceEntityEvent.Move.TargetPlayer event) {
        Player player = event.getTargetEntity();
        if (arena.getOnlinePlayers().contains(player.getUniqueId()) && arena.getCurrentState().equals(ArenaStates.GAME_PLAYING)) {
            if (player.getLocation().getBlockY() < arena.getCenter().getBlockY() && player.get(Keys.GAME_MODE).get().equals(GameModes.SURVIVAL)) {
                arena.broadcast(messages.getMessage("arena.died", "player", player.getName()));
                player.setLocationSafely(arena.getCenter().add(0, 5, 0));
                player.offer(Keys.GAME_MODE, GameModes.SPECTATOR);
                player.playSound(SoundTypes.FALL_BIG, player.getLocation().getPosition(), 3);
                arena.removeAlive(player.getUniqueId());
                if (arena.getAlive().size() == 1) {
                    // The player is the winner
                    Optional<Player> wOpt = arena.resolvePlayer(arena.getAlive().get(0));
                    if (wOpt.isPresent()) {
                        Player winner = wOpt.get();
                        arena.broadcast(messages.getMessage("arena.winner", "player", winner.getName()));
                        if (economyManager.foundEconomy()) {
                            winner.sendMessage(messages.getMessage("arena.reward", "reward",
                                    economyManager.getCurrency().get().format(BigDecimal.valueOf(25)), "reason", Text.of("winning")));
                            economyManager.addCurrency(winner.getUniqueId(), BigDecimal.valueOf(25));
                        }
                        arena.arenaStateChange(ArenaStates.GAME_OVER);
                    }
                } else if (arena.getAlive().size() == 0) {
                    // Everyone lost
                    arena.broadcast(messages.getMessage("arena.winner", "player", "Nobody"));
                    arena.arenaStateChange(ArenaStates.GAME_OVER);
                }
            }
        }
    }

    @Listener
    public void onRightClick(InteractBlockEvent event, @First Player player) {
        if (!cooldown.contains(player.getUniqueId())) {
            Optional<Location<World>> lOpt = event.getTargetBlock().getLocation();
            if (lOpt.isPresent()) {
                Location<World> targetLoc = lOpt.get();
                if (arena.getOnlinePlayers().contains(player.getUniqueId()) && arena.getBlocks().contains(targetLoc)) {
                    if (arena.getCurrentState().equals(ArenaStates.GAME_PLAYING)) {
                        targetLoc.getExtent().setBlockType(targetLoc.getBlockPosition(), BlockTypes.AIR, true,
                                Cause.of(NamedCause.owner(UltimateSpleef.access.pluginContainer)));
                        player.playSound(SoundTypes.DIG_STONE, targetLoc.getPosition(), 3);
                        cooldown.add(player.getUniqueId());
                        Sponge.getScheduler().createTaskBuilder().execute(c -> {
                            cooldown.remove(player.getUniqueId());
                        }).async().delay(400, TimeUnit.MILLISECONDS).submit(UltimateSpleef.access);
                    }
                }
            }
        }
    }
}
