package io.github.flibio.ultimatespleef;

import org.spongepowered.api.Sponge;

import org.spongepowered.api.entity.living.player.Player;
import io.github.flibio.minigamecore.arena.Arena;

public class PreArena extends Arena {

    public PreArena(String name, boolean isDedicated) {
        super(name, Sponge.getGame(), UltimateSpleef.access);
        getData().setVariable("dedicated", Boolean.class, isDedicated);
        getData().setTriggerPlayerEvents(false);
        getData().setVariable("shape", String.class, "circle");
    }

    @Override
    public void addOnlinePlayer(Player arg0) {

    }

    @Override
    public void removeOnlinePlayer(Player arg0) {

    }

}
