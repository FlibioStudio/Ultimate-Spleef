package me.flibio.ultimatespleef;

import me.flibio.minigamecore.arena.Arena;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;

public class UArena extends Arena {
	
	public enum ArenaShapeType {
		CIRCLE
	}

	public UArena(String arenaName, ArenaShapeType type) {
		super(arenaName, Sponge.getGame(), USpleef.access);
	}
	
	@Override
	public void addOnlinePlayer(Player player) {
		
	}
	
	@Override
	public void removeOnlinePlayer(Player player) {
		
	}
}
