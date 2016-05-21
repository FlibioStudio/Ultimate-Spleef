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
package io.github.flibio.ultimatespleef.commands;

import org.spongepowered.api.block.BlockTypes;

import io.github.flibio.minigamecore.arena.ArenaData;
import io.github.flibio.minigamecore.arena.ArenaManager;
import io.github.flibio.ultimatespleef.PreArena;
import io.github.flibio.ultimatespleef.UArena;
import io.github.flibio.ultimatespleef.UltimateSpleef;
import io.github.flibio.utils.commands.BaseCommandExecutor;
import io.github.flibio.utils.commands.Command;
import io.github.flibio.utils.commands.ParentCommand;
import io.github.flibio.utils.message.MessageStorage;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.command.spec.CommandSpec.Builder;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

@ParentCommand(parentCommand = SpleefCommand.class)
@Command(aliases = {"create"}, permission = "ultimatespleef.admin.create")
public class CreateCommand extends BaseCommandExecutor<Player> {

    private MessageStorage messages = UltimateSpleef.getMessageStorage();
    private ArenaManager arenaManager = UltimateSpleef.access.minigame.getArenaManager();
    private PreArena pArena;

    @Override
    public Builder getCommandSpecBuilder() {
        return CommandSpec.builder()
                .arguments(GenericArguments.string(Text.of("name")), GenericArguments.bool(Text.of("dedicated")))
                .executor(this);
    }

    @Override
    public void run(Player src, CommandContext args) {
        Optional<String> sOpt = args.<String>getOne("name");
        Optional<Boolean> bOpt = args.<Boolean>getOne("dedicated");
        if (sOpt.isPresent() && bOpt.isPresent()) {
            String name = sOpt.get();
            boolean dedicated = bOpt.get();
            // Check if the arena is dedicated or not
            if (dedicated) {
                // Check if the server already has a dedicated arena
                if (arenaManager.getArenas().size() > 0) {
                    src.sendMessage(messages.getMessage("command.create.exists"));
                } else {
                    pArena = new PreArena(name, true);
                    arenaManager.addArena(pArena);
                    creationUi(src);
                }
            } else {
                if (arenaManager.getArena(name).isPresent()) {
                    src.sendMessage(messages.getMessage("command.create.exists"));
                } else {
                    pArena = new PreArena(name, false);
                    arenaManager.addArena(pArena);
                    creationUi(src);
                }
            }
        } else {
            src.sendMessage(messages.getMessage("command.error"));
        }
    }

    private void creationUi(Player player) {
        ArenaData data = pArena.getData();
        if (isAllDataPresent()) {
            Sponge.getServer().getOnlinePlayers().forEach(p -> {
                p.kick(messages.getMessage("command.create.finishing"));
            });
            UArena arena = new UArena(data.getName());
            pArena.getData().setTriggerPlayerEvents(true);
            arena.overrideData(pArena.getData());
            arena.initialize();
            arenaManager.removeArena(pArena.getData().getName());
            arenaManager.addArena(arena);
            arenaManager.saveArenaData(arena.getData());
        }
        player.sendMessage(messages.getMessage("command.create.headline"));
        player.sendMessage(messages.getMessage("command.create.name", "name", data.getName()));
        player.sendMessage(messages.getMessage("command.create.dedicated", "dedicated", data.getVariable("dedicated", Boolean.class).get()
                .toString()));
        player.sendMessage(messages.getMessage("command.create.shape", "shape", data.getVariable("shape", String.class).get()));
        // Check if lobby location is present
        if (data.getLocation("lobby").isPresent()) {
            player.sendMessage(messages.getMessage("command.create.lobbyloc", "location", readableLoc(data.getLocation("lobby").get())));
        } else {
            Text button = Text.of(TextColors.GRAY, "[", TextColors.GREEN, "SET", TextColors.GRAY, "]").toBuilder()
                    .onClick(TextActions.executeCallback(c -> {
                        data.setLocation("lobby", player.getLocation().sub(0, 1, 0));
                        creationUi(player);
                    }))
                    .onHover(TextActions.showText(messages.getMessage("command.create.hoverinfo", "where", "being stood on")))
                    .build();
            player.sendMessage(messages.getMessage("command.create.lobbyloc", "location", "").toBuilder().append(button).build());
        }
        // Check if circle center is present
        if (data.getLocation("circlecenter").isPresent()) {
            player.sendMessage(messages.getMessage("command.create.circlecenter", "location", readableLoc(data.getLocation("circlecenter").get())));
        } else {
            Text button = Text.of(TextColors.GRAY, "[", TextColors.GREEN, "SET", TextColors.GRAY, "]").toBuilder()
                    .onClick(TextActions.executeCallback(c -> {
                        data.setLocation("circlecenter", player.getLocation().sub(0, 1, 0));
                        creationUi(player);
                    }))
                    .onHover(TextActions.showText(messages.getMessage("command.create.hoverinfo", "where", "being stood on")))
                    .build();
            player.sendMessage(messages.getMessage("command.create.circlecenter", "location", "").toBuilder().append(button).build());
        }
        // Check if the circle edge is present
        if (data.getLocation("circleedge").isPresent()) {
            player.sendMessage(messages.getMessage("command.create.circleedge", "location", readableLoc(data.getLocation("circleedge").get())));
        } else {
            Text button = Text.of(TextColors.GRAY, "[", TextColors.GREEN, "SET", TextColors.GRAY, "]").toBuilder()
                    .onClick(TextActions.executeCallback(c -> {
                        data.setLocation("circleedge", player.getLocation().sub(0, 1, 0));
                        creationUi(player);
                    }))
                    .onHover(TextActions.showText(messages.getMessage("command.create.hoverinfo", "where", "being stood on")))
                    .build();
            player.sendMessage(messages.getMessage("command.create.circleedge", "location", "").toBuilder().append(button).build());
        }
        if (!data.getVariable("dedicated", Boolean.class).get()) {
            // Check if the join sign is present
            if (data.getLocation("joinsign").isPresent()) {
                player.sendMessage(messages
                        .getMessage("command.create.joinsign", "location", readableLoc(data.getLocation("joinsign").get())));
            } else {
                Text button = Text.of(TextColors.GRAY, "[", TextColors.GREEN, "SET", TextColors.GRAY, "]").toBuilder().onClick(
                        TextActions.executeCallback(c -> {
                            Optional<BlockRayHit<World>> rOpt = BlockRay.from(player).filter(BlockRay.onlyAirFilter()).build().end();
                            if (rOpt.isPresent()) {
                                Location<World> loc = rOpt.get().getLocation();
                                if (loc.getBlockType().equals(BlockTypes.WALL_SIGN) || loc.getBlockType().equals(BlockTypes.STANDING_SIGN)) {
                                    data.setLocation("joinsign", loc);
                                    creationUi(player);
                                } else {
                                    player.sendMessage(messages.getMessage("command.create.invalidsign"));
                                }
                            } else {
                                player.sendMessage(messages.getMessage("command.create.invalidsign"));
                            }
                            creationUi(player);
                        }))
                        .onHover(TextActions.showText(messages.getMessage("command.create.hoverinfo", "where", "being looked at")))
                        .build();
                player.sendMessage(messages.getMessage("command.create.joinsign", "location", "").toBuilder().append(button).build());
            }
        }
    }

    private boolean isAllDataPresent() {
        ArenaData data = pArena.getData();
        if (data.getLocation("lobby").isPresent() && data.getLocation("circlecenter").isPresent() && data.getLocation("circleedge").isPresent()) {
            return !data.getVariable("dedicated", Boolean.class).get() ? data.getLocation("joinsign").isPresent() : true;
        } else {
            return false;
        }
    }

    private String readableLoc(Location<World> loc) {
        return loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

}
