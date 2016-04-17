/*
 * This file is part of UltimateSpleef, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2015 - 2016 Flibio
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
package io.github.flibio.ultimatespleef;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.entity.DisplaceEntityEvent;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import io.github.flibio.minigamecore.arena.Arena;
import io.github.flibio.minigamecore.arena.ArenaStates;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class UArena extends Arena {
	
	private int minPlayers = 1;
	private int maxPlayers = 8;
	private Location<World> lobbySpawn;
	private int circleRad;
	private Location<World> circleCenter;
	private ArrayList<Location<World>> blocks = new ArrayList<Location<World>>();
	private ArrayList<Location<World>> usedLocs = new ArrayList<Location<World>>();
	
	private int lobbyCountdownTime = 30;
	private int gameCountdown = 10;
	private Task lobbyCountdownTask;
	private Task gameCountdownTask;
	
	public enum ArenaShapeType {
		CIRCLE
	}

	public UArena(String arenaName) {
		super(arenaName, Sponge.getGame(), UltimateSpleef.access);
	}
	
	public void initialize() {
		lobbySpawn = getData().getLocation("lobby").get();
		circleRad = (int) getData().getLocation("circleedge").get().getPosition().distance(lobbySpawn.getPosition());
		circleCenter = getData().getLocation("circlecenter").get();
		blocks = getCircle(circleCenter,circleRad);
		blocks.forEach(block -> {
		    block.setBlockType(BlockTypes.QUARTZ_BLOCK);
		});

		//On lobby countdown
		addArenaStateRunnable(ArenaStates.LOBBY_COUNTDOWN, new Runnable() {
			@Override
			public void run() {
				broadcast(Text.of(TextColors.GRAY,"Joining the game in ",TextColors.YELLOW,lobbyCountdownTime,TextColors.GRAY," seconds!"));
				lobbyCountdownTask = Sponge.getGame().getScheduler().createTaskBuilder().execute(r -> {
					if(lobbyCountdownTime <= 5 && lobbyCountdownTime!=0){
						broadcastSound(SoundTypes.NOTE_PIANO, 2, 1);
						broadcast(Text.of(TextColors.GRAY,"Joining the game in ",TextColors.YELLOW,lobbyCountdownTime,TextColors.GRAY," seconds!"));
					}
					if(lobbyCountdownTime==20||lobbyCountdownTime==10) {
						broadcast(Text.of(TextColors.GRAY,"Joining the game in ",TextColors.YELLOW,lobbyCountdownTime,TextColors.GRAY," seconds!"));
					}
					if(lobbyCountdownTime==0) {
						//Start the game
						broadcast(Text.of(TextColors.GRAY,"Joining the game!"));
						lobbyCountdownTime = 30;
						arenaStateChange(ArenaStates.GAME_COUNTDOWN);
						lobbyCountdownTask.cancel();
					}
					lobbyCountdownTime--;
				}).interval(1, TimeUnit.SECONDS).submit(UltimateSpleef.access);
			}
		});
		
		//On game countdown
		addArenaStateRunnable(ArenaStates.GAME_COUNTDOWN, new Runnable() {
			@Override
			public void run() {
				//Randomly distribute the players among the arena
				for(Player player : getOnlinePlayers()) {
					player.offer(Keys.GAME_MODE,GameModes.SPECTATOR);
					player.offer(Keys.FLYING_SPEED,0.0);
					Location<World> loc = blocks.get((new Random()).nextInt(blocks.size()));
					while(usedLocs.contains(loc)) {
						loc = blocks.get((new Random()).nextInt(blocks.size()));
					}
					usedLocs.add(loc);
					player.setLocationSafely(loc.add(0, 2, 0));
				}
				usedLocs.clear();
				//Countdown the game time
				gameCountdown = 10;
				broadcast(Text.of(TextColors.GRAY,"The game starts in ",TextColors.YELLOW,gameCountdown,TextColors.GRAY," seconds!"));
				gameCountdownTask = Sponge.getGame().getScheduler().createTaskBuilder().execute(r -> {
					if(gameCountdown <= 5 && gameCountdown!=0){
						broadcast(Text.of(TextColors.GRAY,"The game starts in ",TextColors.YELLOW,gameCountdown,TextColors.GRAY," seconds!"));
						broadcastSound(SoundTypes.NOTE_PIANO, 2, 1);
					}
					if(gameCountdown<=0) {
						for(Player player : getOnlinePlayers()) {
							player.offer(Keys.GAME_MODE,GameModes.SURVIVAL);
							player.offer(Keys.FLYING_SPEED,0.05);
						}
						arenaStateChange(ArenaStates.GAME_PLAYING);
						gameCountdownTask.cancel();
						broadcast(Text.of(TextColors.GRAY,"The game has begun!"));
					}
					gameCountdown--;
				}).interval(1, TimeUnit.SECONDS).submit(UltimateSpleef.access);
			}
		});
	}
	
	@Override
	public void addOnlinePlayer(Player player) {
		if(getOnlinePlayers().size()==maxPlayers) {
			player.kick(Text.of(TextColors.RED,"The game is full!"));
			return;
		}
		if(getCurrentState().equals(ArenaStates.LOBBY_WAITING)||getCurrentState().equals(ArenaStates.LOBBY_COUNTDOWN)) {
			getOnlinePlayers().add(player);
			//Teleport the player to the lobby
			player.setLocationSafely(lobbySpawn.add(0,1,0));
			player.offer(Keys.GAME_MODE,GameModes.SURVIVAL);
			broadcast(Text.of(TextColors.YELLOW,player.getName(),TextColors.GRAY," has joined the game!"));
			if(getOnlinePlayers().size()>=minPlayers) {
				//Start the countdown
				arenaStateChange(ArenaStates.LOBBY_COUNTDOWN);
			}
		} else {
			player.kick(Text.of(TextColors.RED,"The game is in progress!"));
			return;
		}
		
	}
	
	@Override
	public void removeOnlinePlayer(Player player) {
		getOnlinePlayers().remove(player);
		broadcast(Text.of(TextColors.YELLOW,player.getName(),TextColors.GRAY," has left the game!"));
		if(getCurrentState().equals(ArenaStates.LOBBY_COUNTDOWN)&& getOnlinePlayers().size() < minPlayers) {
            lobbyCountdownTask.cancel();
            arenaStateChange(ArenaStates.LOBBY_WAITING);
            Sponge.getGame().getServer().getBroadcastChannel().send(Text.of(TextColors.RED, "The lobby countdown has been cancelled!"));
            lobbyCountdownTime = 30;
        }
        if (getCurrentState().equals(ArenaStates.GAME_PLAYING) && getOnlinePlayers().size() == 0) {
            // Reset the arena
            lobbyCountdownTime = 30;
            for (Location<World> loc : blocks) {
                loc.setBlockType(BlockTypes.QUARTZ_BLOCK);
            }
            gameCountdown = 10;
            arenaStateChange(ArenaStates.LOBBY_WAITING);
            broadcast(Text.of(TextColors.RED, "There are not enough people to play!"));
        }
        if (getCurrentState().equals(ArenaStates.GAME_PLAYING) && getOnlinePlayers().size() < minPlayers) {

        }
    }

    @Listener
    public void onMove(DisplaceEntityEvent.Move.TargetPlayer event) {
        Player player = event.getTargetEntity();
        if (getOnlinePlayers().contains(player) && getCurrentState().equals(ArenaStates.GAME_PLAYING)) {
            if (player.getLocation().getBlockY() < circleCenter.getBlockY() && player.get(Keys.GAME_MODE).get().equals(GameModes.SURVIVAL)) {
                broadcast(Text.of(TextColors.YELLOW, player.getName(), TextColors.GRAY, " has died!"));
                player.setLocation(circleCenter.add(0, 10, 0));
                player.offer(Keys.GAME_MODE, GameModes.SPECTATOR);
            }
        }
    }

    @Listener
    public void onRightClick(InteractBlockEvent event) {
        Optional<Player> playerOptional = event.getCause().first(Player.class);
        if (!playerOptional.isPresent())
            return;
        Player player = playerOptional.get();
        if (getOnlinePlayers().contains(player) && blocks.contains(event.getTargetBlock().getLocation().get())) {
            if (getCurrentState().equals(ArenaStates.GAME_PLAYING)) {
                event.getTargetBlock().getLocation().get().setBlockType(BlockTypes.AIR);
                player.playSound(SoundTypes.DIG_STONE, event.getTargetBlock().getLocation().get().getPosition(), 2);
            }
        }
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

}
