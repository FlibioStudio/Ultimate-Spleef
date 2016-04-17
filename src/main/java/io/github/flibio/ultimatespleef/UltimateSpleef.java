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

import com.google.inject.Inject;
import io.github.flibio.minigamecore.Minigame;
import io.github.flibio.minigamecore.arena.ArenaData;
import io.github.flibio.ultimatespleef.commands.CreateCommand;
import io.github.flibio.ultimatespleef.commands.SpleefCommand;
import io.github.flibio.utils.commands.CommandLoader;
import io.github.flibio.utils.message.MessageStorage;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.serializer.TextSerializers;

@Plugin(id = "ultimatespleef", name = "Ultimate Spleef", version = "0.1.0")
public class UltimateSpleef {

    @Inject private Logger logger;

    @Inject private Game game;

    public String version = UltimateSpleef.class.getAnnotation(Plugin.class).version();

    public Minigame minigame;

    public static UltimateSpleef access;

    private static MessageStorage messageStorage;

    @Listener
    public void onServerInitialize(GameStartingServerEvent event) {
        logger.info("Ultimate Spleef v" + version + " is enabling!");
        access = this;

        minigame = Minigame.create("UltimateSpleef", this).get();
        messageStorage = MessageStorage.createInstance(this);
        messageStorage.defaultMessages("messages");

        CommandLoader.registerCommands(this, TextSerializers.FORMATTING_CODE.serialize(messageStorage.getMessage("command.invalidsource")),
                new SpleefCommand(),
                new CreateCommand()
                );

        for (ArenaData arenaData : minigame.getArenaManager().loadArenaData()) {
            UArena uarena = new UArena(arenaData.getName());
            uarena.overrideData(arenaData);
            uarena.initialize();
            minigame.getArenaManager().addArena(uarena);
        }
    }

    public static MessageStorage getMessageStorage() {
        return messageStorage;
    }

    public Logger getLogger() {
        return logger;
    }

    public Game getGame() {
        return game;
    }

}
