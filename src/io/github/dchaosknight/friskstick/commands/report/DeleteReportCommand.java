package io.github.dchaosknight.friskstick.commands.report;

import io.github.dchaosknight.friskstick.data.ReportData;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class DeleteReportCommand implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {

        // Gets the argument minus one (to compensate for the reports being displayed starting at 1, not 0)
        int index = args.<Integer>getOne("index").get() - 1;

        if (index < 0) {

            src.sendMessage(Text.of(TextColors.RED, "Please enter a number above zero."));
            return CommandResult.empty();

        }

        if (index >= ReportData.getAllReports().size()) {

            src.sendMessage(Text.of(TextColors.RED, "There is no report with that number."));
            return CommandResult.empty();

        }

        ReportData.removeReport(index);

        src.sendMessage(Text.of(TextColors.GREEN, "Report ", TextColors.GOLD, String.valueOf(index + 1), TextColors.GREEN, " deleted!"));

        return CommandResult.success();

    }

}
