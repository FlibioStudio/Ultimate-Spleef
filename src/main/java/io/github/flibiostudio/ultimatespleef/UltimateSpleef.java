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

package io.github.flibiostudio.ultimatespleef;

import io.github.flibiostudio.ultimatespleef.arena.UArena;
import io.github.flibiostudio.ultimatespleef.commands.CreateCommand;
import io.github.flibiostudio.ultimatespleef.commands.SpleefCommand;

import com.google.inject.Inject;
import io.github.flibio.minigamecore.Minigame;
import io.github.flibio.minigamecore.arena.ArenaData;
import io.github.flibio.utils.commands.CommandLoader;
import io.github.flibio.utils.message.MessageStorage;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.Optional;

@Plugin(id = "ultimatespleef")
public class UltimateSpleef {

    @Inject private Logger logger;

    @Inject private Game game;

    @Inject public PluginContainer pluginContainer;

    public Minigame minigame;

    public static UltimateSpleef access;

    private static MessageStorage messageStorage;

    @Listener
    public void onServerInitialize(GameStartingServerEvent event) {
        logger.info("Ultimate Spleef is enabling!");
        access = this;

        minigame = Minigame.create("UltimateSpleef", this).get();
        messageStorage = MessageStorage.createInstance(this);
        messageStorage.defaultMessages("messages");

        CommandLoader.registerCommands(this, TextSerializers.FORMATTING_CODE.serialize(messageStorage.getMessage("command.invalidsource")),
                new SpleefCommand(),
                new CreateCommand()
                );

        for (ArenaData arenaData : minigame.getArenaManager().loadArenaData()) {
            Optional<UArena> uarena = UArena.createArena(arenaData.getName(), arenaData);
            if (uarena.isPresent())
                minigame.getArenaManager().addArena(uarena.get());
        }

        if (!minigame.getEconomyManager().foundEconomy()) {
            logger.warn("Could not find an economy plugin! Players will not be rewarded!");
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
