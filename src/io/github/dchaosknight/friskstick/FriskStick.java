package io.github.dchaosknight.friskstick;

import com.google.inject.Inject;
import io.github.dchaosknight.friskstick.bstats.Metrics;
import io.github.dchaosknight.friskstick.commands.report.DeleteReportCommand;
import io.github.dchaosknight.friskstick.commands.report.ListReportCommand;
import io.github.dchaosknight.friskstick.commands.report.ReportCommand;
import io.github.dchaosknight.friskstick.data.ReportData;
import io.github.dchaosknight.friskstick.listeners.BeatdownListener;
import io.github.dchaosknight.friskstick.listeners.FriskListener;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/*  TODO
    - Reporting system
        - Make sure player can't report themselves (completed)
        - Possibly add a date and time? (completed)
        - Switch to UUID instead of storing raw names (completed)
    - Commands:
        - Help
        - Report (completed)
        - Update Check
    - Update Checking (if/when possible)
    - Beatdown Mode
        - Basic features
            - Class to track players "on the run" (completed)
            - Config messages (completed)
            - Time limit (completed)
        - Once feature is available: replace health system with absorption health

 */

@Plugin(id = "friskstick", name = "FriskStick", version = "1.0.1", description = "A plugin to frisk players!",
        authors = {"dchaosknight"})
public class FriskStick {

    @Inject
    private Game game;

    @Inject
    private Logger logger;

    @Inject
    @DefaultConfig(sharedRoot = false)
    private Path configFile;

    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configDir;

    private Path reportFile;

    private ConfigurationNode config;

    // bStats instance
    @Inject
    private Metrics metrics;

    @Listener
    public void onPreInit(GamePreInitializationEvent event) {

        // Config setup

        if (Files.notExists(configFile)) {

            try {

                game.getAssetManager().getAsset(this, "friskstick.conf").orElseThrow(IOException::new).copyToFile(configFile);

            } catch (IOException e) {

                logger.error("Could not create default configuration file!");
                e.printStackTrace();

            }

        }

        try {

            config = HoconConfigurationLoader.builder().setPath(configFile).build().load();

        } catch (IOException e) {

            logger.error("Could not load configuration file!");
            e.printStackTrace();

        }

        // Report file loading

        if (config.getNode("enable-reporting").getBoolean()) {

            try {

                reportFile = Paths.get(configDir.toString(), "reports.conf");

                if (Files.notExists(reportFile)) {

                    Files.createFile(reportFile);

                }

                ReportData.loadReportsFromFile(reportFile);

            } catch (IOException | ObjectMappingException e) {

                logger.error("Could not load report file!");
                e.printStackTrace();

            }

        }

    }

    @Listener
    public void onInit(GameInitializationEvent event) {

        // Event listeners
        game.getEventManager().registerListeners(this, new FriskListener(this));
        game.getEventManager().registerListeners(this, new BeatdownListener(this));

        // Commands

        if (config.getNode("enable-reporting").getBoolean()) {

            CommandSpec listReportCommand = CommandSpec.builder().executor(new ListReportCommand(this))
                    .permission("friskstick.report.list").build();

            CommandSpec deleteReportCommand = CommandSpec.builder().executor(new DeleteReportCommand())
                    .permission("friskstick.report.delete").arguments(
                            GenericArguments.onlyOne(GenericArguments.integer(Text.of("index")))
                    ).build();

            CommandSpec reportCommand = CommandSpec.builder().executor(new ReportCommand(this)).arguments(
                    GenericArguments.onlyOne(GenericArguments.player(Text.of("player")))
            ).child(listReportCommand, "list").child(deleteReportCommand, "delete", "remove", "del", "rm").build();

            game.getCommandManager().register(this, reportCommand, "report");

        }

    }

    @Listener
    public void onShutdown(GameStoppingServerEvent event) {

        if (config.getNode("enable-reporting").getBoolean()) {

            try {

                ReportData.saveReportsToFile(reportFile);

            } catch (IOException e) {

                logger.error("Could not save reports to file!");
                e.printStackTrace();

            }

        }

    }

    public Game getGame() {

        return game;

    }

    public Logger getLogger() {

        return logger;

    }

    public ConfigurationNode getConfig() {

        return config;

    }

}
