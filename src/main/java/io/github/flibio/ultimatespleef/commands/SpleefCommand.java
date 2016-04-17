package io.github.flibio.ultimatespleef.commands;

import io.github.flibio.ultimatespleef.UltimateSpleef;
import io.github.flibio.utils.commands.AsyncCommand;
import io.github.flibio.utils.commands.BaseCommandExecutor;
import io.github.flibio.utils.commands.Command;
import io.github.flibio.utils.message.MessageStorage;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.command.spec.CommandSpec.Builder;
import org.spongepowered.api.entity.living.player.Player;

@AsyncCommand
@Command(aliases = {"spleef"}, permission = "ultimatespleef.admin")
public class SpleefCommand extends BaseCommandExecutor<Player> {

    private MessageStorage messageStorage = UltimateSpleef.getMessageStorage();

    @Override
    public Builder getCommandSpecBuilder() {
        return CommandSpec.builder()
                .executor(this);
    }

    @Override
    public void run(Player player, CommandContext args) {
        player.sendMessage(messageStorage.getMessage("command.usage", "command", "/spleef", "subcommands", "create"));
    }
}
