package io.github.dchaosknight.friskstick.commands.report;

import io.github.dchaosknight.friskstick.FriskStick;
import io.github.dchaosknight.friskstick.data.ReportData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.text.ParseException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class ListReportCommand implements CommandExecutor {

    private FriskStick plugin;

    public ListReportCommand(FriskStick plugin) {

        this.plugin = plugin;

    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {

        List<List<UUID>> reports = ReportData.getAllReports();

        if (reports.size() == 0) {

           src.sendMessage(Text.of(TextColors.GOLD, "There are no reports to list."));
           return CommandResult.success();

        }

        reports.forEach(report -> {

            try {

                String timestamp = ReportData.getFormattedDateOfReport(plugin.getConfig().getNode("timestamp").getString(),
                        reports.indexOf(report));
                String reporterName = Sponge.getServer().getGameProfileManager().get(report.get(0)).get().getName().get();
                String reportedName = Sponge.getServer().getGameProfileManager().get(report.get(1)).get().getName().get();

                src.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(plugin.getConfig().getNode("report-list-msg")
                        .getString().replaceAll("%index%", String.valueOf(reports.indexOf(report) + 1))
                        .replaceAll("%reporter%", reporterName).replaceAll("%reported%", reportedName)
                        .replaceAll("%timestamp%", timestamp)));

            } catch (InterruptedException | ExecutionException e) {

                plugin.getLogger().error("Could not access user profiles!");
                e.printStackTrace();

            } catch (ParseException e) {

                plugin.getLogger().error("Could not format the report dates!");
                e.printStackTrace();

            }

        });

        return CommandResult.success();

    }

}
