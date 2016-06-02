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
package io.github.flibio.ultimatespleef.arena;

import org.spongepowered.api.entity.living.player.Player;
import io.github.flibio.minigamecore.arena.ArenaStates;
import io.github.flibio.ultimatespleef.UltimateSpleef;
import io.github.flibio.utils.message.MessageStorage;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class GameCountdownRunnable implements Runnable {

    private MessageStorage messages = UltimateSpleef.getMessageStorage();

    private Task countdownTask;
    private int countdownTime = 5;
    private UArena arena;

    public GameCountdownRunnable(UArena arena) {
        this.arena = arena;
    }

    public void run() {
        List<Location<World>> usedLocs = new ArrayList<>();
        List<Location<World>> blocks = arena.getBlocks();
        arena.resolvePlayers(arena.getOnlinePlayers()).forEach(player -> {
            player.offer(Keys.GAME_MODE, GameModes.SPECTATOR);
            player.offer(Keys.FLYING_SPEED, 0.0);
            Location<World> loc = blocks.get((new Random()).nextInt(blocks.size()));
            while (usedLocs.contains(loc)) {
                loc = blocks.get((new Random()).nextInt(blocks.size()));
            }
            usedLocs.add(loc);
            player.setLocationSafely(loc.add(0, 2, 0));
        });
        usedLocs.clear();
        countdownTime = 5;
        countdownTask = Sponge.getScheduler().createTaskBuilder().execute(t -> {
            String label = "seconds";
            if (countdownTime == 1) {
                label = "second";
            }
            if (countdownTime > 0) {
                arena.broadcastSound(SoundTypes.CLICK, 5, 1);
                arena.broadcast(messages.getMessage("arena.gamestarting", "count", String.valueOf(countdownTime), "label", label));
            }
            if (countdownTime == 0) {
                arena.broadcastSound(SoundTypes.NOTE_PIANO, 5, 1);
                arena.broadcast(messages.getMessage("arena.gamestarted"));
                for (Player player : arena.resolvePlayers(arena.getOnlinePlayers())) {
                    player.offer(Keys.GAME_MODE, GameModes.SURVIVAL);
                    player.offer(Keys.FLYING_SPEED, 0.05);
                }
                arena.arenaStateChange(ArenaStates.GAME_PLAYING);
                cancelCountdown();
            }
            countdownTime--;
        }).interval(1, TimeUnit.SECONDS).submit(UltimateSpleef.access);
    }

    public void cancelCountdown() {
        if (countdownTask != null)
            countdownTask.cancel();
        countdownTime = 30;
    }

}
