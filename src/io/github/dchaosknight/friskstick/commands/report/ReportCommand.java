package io.github.dchaosknight.friskstick.commands.report;

import io.github.dchaosknight.friskstick.FriskStick;
import io.github.dchaosknight.friskstick.data.ReportData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

public class ReportCommand implements CommandExecutor {

    private FriskStick plugin;

    public ReportCommand(FriskStick plugin) {

        this.plugin = plugin;

    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {

        if (args.<Player>getOne("player").isPresent() && src instanceof Player && src.hasPermission("friskstick.report.send")) {

            Player reported = args.<Player>getOne("player").get();
            Player reporter = (Player)src;

            if (reported == reporter) {

                reporter.sendMessage(Text.of(TextColors.RED, "You cannot report yourself."));
                return CommandResult.empty();

            }

            if (!ReportData.addReport(reporter.getUniqueId(), reported.getUniqueId())) {

                reporter.sendMessage(Text.of(TextColors.RED, "You have already reported that player."));
                return CommandResult.empty();

            }

            reporter.sendMessage(Text.of(TextColors.GREEN, "Report submitted!"));

            Sponge.getServer().getOnlinePlayers().stream().filter(player -> player.hasPermission("friskstick.report.receive"))
                    .forEach(player -> player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(plugin.getConfig()
                    .getNode("report-msg").getString().replaceAll("%reporter%", reporter.getName())
                            .replaceAll("%reported%", reported.getName()))));

            return CommandResult.success();

        } else if (!(src instanceof Player)) {

            src.sendMessage(Text.of(TextColors.RED, "Only players can use this command!"));

        } else if (!src.hasPermission("friskstick.report.send")) {

            src.sendMessage(Text.of(TextColors.RED, "You do not have permission to use this command!"));

        }

        return CommandResult.empty();

    }

}
