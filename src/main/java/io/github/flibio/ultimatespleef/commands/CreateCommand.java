package io.github.flibio.ultimatespleef.commands;

import io.github.flibio.minigamecore.arena.ArenaData;
import io.github.flibio.ultimatespleef.PreArena;
import io.github.flibio.ultimatespleef.UArena;
import io.github.flibio.ultimatespleef.UltimateSpleef;
import io.github.flibio.utils.commands.BaseCommandExecutor;
import io.github.flibio.utils.commands.Command;
import io.github.flibio.utils.commands.ParentCommand;
import io.github.flibio.utils.message.MessageStorage;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.command.spec.CommandSpec.Builder;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

@ParentCommand(parentCommand = SpleefCommand.class)
@Command(aliases = {"create"}, permission = "ultimatespleef.admin.create")
public class CreateCommand extends BaseCommandExecutor<Player> {

    private MessageStorage messages = UltimateSpleef.getMessageStorage();
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
            // TODO check if arena name exists
            pArena = new PreArena(sOpt.get(), bOpt.get());
            UltimateSpleef.access.minigame.getArenaManager().addArena(pArena);
            creationUi(src);
        } else {
            src.sendMessage(messages.getMessage("command.error"));
        }
    }

    private void creationUi(Player player) {
        ArenaData data = pArena.getData();
        if (isAllDataPresent()) {
            UArena arena = new UArena(data.getName());
            pArena.getData().setTriggerPlayerEvents(true);
            arena.overrideData(pArena.getData());
            arena.initialize();
            UltimateSpleef.access.minigame.getArenaManager().removeArena(pArena.getData().getName());
            UltimateSpleef.access.minigame.getArenaManager().addArena(arena);
            UltimateSpleef.access.minigame.getArenaManager().saveArenaData(arena.getData());
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
                        data.setLocation("lobby", player.getLocation());
                        creationUi(player);
                    })).build();
            player.sendMessage(messages.getMessage("command.create.lobbyloc", "location", button));
        }
        // Check if circle center is present
        if (data.getLocation("circlecenter").isPresent()) {
            player.sendMessage(messages.getMessage("command.create.circlecenter", "location", readableLoc(data.getLocation("circlecenter").get())));
        } else {
            Text button = Text.of(TextColors.GRAY, "[", TextColors.GREEN, "SET", TextColors.GRAY, "]").toBuilder()
                    .onClick(TextActions.executeCallback(c -> {
                        data.setLocation("circlecenter", player.getLocation());
                        creationUi(player);
                    })).build();
            player.sendMessage(messages.getMessage("command.create.circlecenter", "location", button));
        }
        // Check if the circle edge is present
        if (data.getLocation("circleedge").isPresent()) {
            player.sendMessage(messages.getMessage("command.create.circleedge", "location", readableLoc(data.getLocation("circleedge").get())));
        } else {
            Text button = Text.of(TextColors.GRAY, "[", TextColors.GREEN, "SET", TextColors.GRAY, "]").toBuilder()
                    .onClick(TextActions.executeCallback(c -> {
                        data.setLocation("circleedge", player.getLocation());
                        creationUi(player);
                    })).build();
            player.sendMessage(messages.getMessage("command.create.circleedge", "location", button));
        }
        if (!data.getVariable("dedicated", Boolean.class).get()) {
            // Check if the join sign is present
            if (data.getLocation("joinsign").isPresent()) {
                player.sendMessage(messages
                        .getMessage("command.create.joinsign", "location", readableLoc(data.getLocation("joinsign").get())));
            } else {
                Text button = Text.of(TextColors.GRAY, "[", TextColors.GREEN, "SET", TextColors.GRAY, "]").toBuilder()
                        .onClick(TextActions.executeCallback(c -> {
                            data.setLocation("joinsign", player.getLocation());
                            creationUi(player);
                        })).build();
                player.sendMessage(messages.getMessage("command.create.joinsign", "location", button));
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
