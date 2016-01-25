package me.flibio.ultimatespleef;

import me.flibio.minigamecore.arena.Arena;
import me.flibio.minigamecore.arena.ArenaStates;

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

import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;


public class UArena extends Arena {
	
	private int minPlayers = 1;
	private int maxPlayers = 8;
	private Location<World> lobbySpawn;
	private boolean dedicated;
	private int circleRad;
	private Location<World> circleCenter;
	private ArrayList<Location<World>> blocks = new ArrayList<Location<World>>();
	private ArrayList<Location<World>> usedLocs = new ArrayList<Location<World>>();;
	
	private int lobbyCountdownTime = 30;
	private int gameCountdown = 10;
	private Task lobbyCountdownTask;
	private Task gameCountdownTask;
	
	public enum ArenaShapeType {
		CIRCLE
	}

	public UArena(String arenaName, ArenaShapeType type) {
		super(arenaName, Sponge.getGame(), USpleef.access);
	}
	
	public void initialize() {
		lobbySpawn = getData().getLocation("lobbySpawn").get();
		dedicated = (boolean) getData().getVariable("dedicatedServer").get();
		circleRad = (int) getData().getVariable("circlerad").get();
		circleCenter = getData().getLocation("circlecenter").get();
		blocks = getCircle(circleCenter,circleRad);
		
		addArenaStateRunnable(ArenaStates.GAME_COUNTDOWN, new Runnable() {
			@Override
			public void run() {
				//Countdown the game time
				gameCountdown = 10;
				gameCountdownTask = Sponge.getGame().getScheduler().createTaskBuilder().execute(r -> {
					broadcast(Text.of(TextColors.GRAY,"The game starts in ",TextColors.YELLOW,gameCountdown,TextColors.GRAY," seconds!"));
					if(gameCountdown <= 5){
						broadcastSound(SoundTypes.NOTE_BASS_GUITAR, 2, 1);
					}
					if(lobbyCountdownTime<=0) {
						arenaStateChange(ArenaStates.GAME_PLAYING);
						gameCountdownTask.cancel();
					}
					gameCountdown--;
				}).interval(1, TimeUnit.SECONDS).submit(USpleef.access);
			}
		});
	}
	
	@Override
	public void addOnlinePlayer(Player player) {
		if(getOnlinePlayers().size()==maxPlayers) {
			player.kick(Text.of(TextColors.RED,"The game is full!"));
			return;
		}
		if(getArenaState().equals(ArenaStates.LOBBY_WAITING)||getArenaState().equals(ArenaStates.LOBBY_COUNTDOWN)) {
			getOnlinePlayers().add(player);
			//Teleport the player to the lobby
			player.setLocationSafely(lobbySpawn.add(0,1,0));
			player.offer(Keys.GAME_MODE,GameModes.SURVIVAL);
			broadcast(Text.of(TextColors.YELLOW,player.getName(),TextColors.GRAY," has joined the game!"));
			if(getOnlinePlayers().size()>=minPlayers) {
				//Start the countdown
				arenaStateChange(ArenaStates.LOBBY_COUNTDOWN);
				broadcast(Text.of(TextColors.GRAY,"The game starts in ",TextColors.YELLOW,lobbyCountdownTime,TextColors.GRAY," seconds!"));
				lobbyCountdownTask = Sponge.getGame().getScheduler().createTaskBuilder().execute(r -> {
					if(lobbyCountdownTime <= 10){
						broadcastSound(SoundTypes.NOTE_PIANO, 2, 1);
						broadcast(Text.of(TextColors.GRAY,"The game starts in ",TextColors.YELLOW,lobbyCountdownTime,TextColors.GRAY," seconds!"));
					}
					if(lobbyCountdownTime==20||lobbyCountdownTime==15) {
						broadcast(Text.of(TextColors.GRAY,"The game starts in ",TextColors.YELLOW,lobbyCountdownTime,TextColors.GRAY," seconds!"));
					}
					if(lobbyCountdownTime==0) {
						//Start the game
						lobbyCountdownTime = 30;
						for(Location<World> b : blocks) {
							b.setBlockType(BlockTypes.QUARTZ_BLOCK);
						}
						//Randomly distribute the players among the arena
						for(Player oPlayer : getOnlinePlayers()) {
							Location<World> loc = blocks.get((new Random()).nextInt(blocks.size()));
							while(usedLocs.contains(loc)) {
								loc = blocks.get((new Random()).nextInt(blocks.size()));
							}
							usedLocs.add(loc);
							oPlayer.setLocation(loc);
						}
						usedLocs.clear();
						arenaStateChange(ArenaStates.GAME_COUNTDOWN);
						lobbyCountdownTask.cancel();
					}
					lobbyCountdownTime--;
				}).interval(1, TimeUnit.SECONDS).submit(USpleef.access);
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
		if(getArenaState().equals(ArenaStates.LOBBY_COUNTDOWN)&&getOnlinePlayers().size()<minPlayers) {
			arenaStateChange(ArenaStates.LOBBY_WAITING);
			Sponge.getGame().getServer().getBroadcastChannel().send(Text.of(TextColors.RED,"The lobby countdown has been cancelled!"));
			lobbyCountdownTime = 30;
		}
	}
	
	@Listener
	public void onMove(DisplaceEntityEvent.Move.TargetPlayer event) {
		Player player = event.getTargetEntity();
		if(getOnlinePlayers().contains(player)&&getArenaState().equals(ArenaStates.GAME_COUNTDOWN)) event.setCancelled(true);
		if(getOnlinePlayers().contains(player)&&getArenaState().equals(ArenaStates.GAME_PLAYING)) {
			if(player.getLocation().getBlockY()<circleCenter.getBlockY()) {
				broadcast(Text.of(TextColors.YELLOW,player.getName(),TextColors.GRAY," has died!"));
				player.setLocation(circleCenter.add(0, 10, 0));
				player.offer(Keys.GAME_MODE, GameModes.SPECTATOR);
			}
		}
	}
	
	@Listener
	public void onRightClick(InteractBlockEvent event) {
		Optional<Player> playerOptional = event.getCause().first(Player.class);
		if(!playerOptional.isPresent()) return;
		Player player = playerOptional.get();
		if(getOnlinePlayers().contains(player)&&blocks.contains(event.getTargetBlock().getLocation().get())) {
			if(getArenaState().equals(ArenaStates.GAME_PLAYING)) {
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
		for(int x = cx - r; x <= cx +r; x++) {
			for(int z = cz - r; z <= cz +r; z++) {
				if((cx - x) * (cx -x) + (cz - z) * (cz - z) <= rSquared) {
					locations.add(new Location<World>(w,x,cy,z));
				}
			}
		}
		return locations;
	}
	
}
