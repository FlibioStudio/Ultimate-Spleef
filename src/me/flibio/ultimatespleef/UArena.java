package me.flibio.ultimatespleef;

import me.flibio.minigamecore.arena.Arena;
import me.flibio.minigamecore.arena.ArenaStates;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.concurrent.TimeUnit;

public class UArena extends Arena {
	
	private int minPlayers = 2;
	private int maxPlayers = 8;
	private Location<World> lobbySpawn;
	private boolean dedicated;
	private int currentTime = 30;
	private Task countdownTask;
	
	public enum ArenaShapeType {
		CIRCLE
	}

	public UArena(String arenaName, ArenaShapeType type) {
		super(arenaName, Sponge.getGame(), USpleef.access);
		lobbySpawn = getData().getLocation("lobbySpawn").get();
		dedicated = (boolean) getData().getVariable("dedicatedServer").get();
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
			if(getOnlinePlayers().size()>=minPlayers) {
				//Start the countdown
				arenaStateChange(ArenaStates.LOBBY_COUNTDOWN);
				countdownTask = Sponge.getGame().getScheduler().createTaskBuilder().execute(r -> {
					broadcast(Text.of(TextColors.GRAY,"The game starts in ",TextColors.YELLOW,currentTime,TextColors.GRAY," seconds!"));
					if(currentTime <= 10){
						broadcastSound(SoundTypes.ORB_PICKUP, 2, 1);
					}
					if(currentTime==0) {
						//Start the game
						currentTime = 30;
						arenaStateChange(ArenaStates.GAME_COUNTDOWN);
						countdownTask.cancel();
						return;
					}
					currentTime--;
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
		if(getArenaState().equals(ArenaStates.LOBBY_COUNTDOWN)&&getOnlinePlayers().size()<minPlayers) {
			arenaStateChange(ArenaStates.LOBBY_WAITING);
			Sponge.getGame().getServer().getBroadcastChannel().send(Text.of(TextColors.RED,"The lobby countdown has been cancelled!"));
			currentTime = 30;
		}
	}
	
}
