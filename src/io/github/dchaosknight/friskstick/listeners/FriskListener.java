package io.github.dchaosknight.friskstick.listeners;

import com.flowpowered.math.vector.Vector3d;
import io.github.dchaosknight.friskstick.FriskStick;
import io.github.dchaosknight.friskstick.data.BeatdownData;
import io.github.dchaosknight.friskstick.util.FriskUtil;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.InteractEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class FriskListener {

    private FriskStick plugin;
    private long lastTimeCalled = 0;

    public FriskListener(FriskStick plugin) {

        this.plugin = plugin;

    }

    @Listener
    public void onFriskRequest(InteractEntityEvent.Secondary event, @First Player frisker, @Getter("getTargetEntity") Player frisked) {

        // Prevent duplicate events
        if ((System.currentTimeMillis() - lastTimeCalled) <= 100) {

            return;

        }

        lastTimeCalled = System.currentTimeMillis();

        // Frisk request
        if (frisker.getItemInHand(HandTypes.MAIN_HAND).orElse(ItemStack.of(ItemTypes.BEDROCK, 1)).getItem() == ItemTypes.STICK
                && frisker.hasPermission("friskstick.frisk") && !frisked.hasPermission("friskstick.bypass")) {

            ConfigurationNode config = plugin.getConfig();

            // If beatdown mode is enabled...
            if (config.getNode("enable-beatdown-mode").getBoolean()) {

                // If the player is already on the run or still needs to react to frisking, don't do anything
                if (BeatdownData.isRunning(frisked.getUniqueId()) || BeatdownData.awaitingResponseFrom(frisked.getUniqueId())) {

                    return;

                }

                // How long (in seconds) the plugin should give the player to comply
                int gracePeriod = config.getNode("grace-period").getInt();

                frisked.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(config
                        .getNode("frisk-request-player-msg").getString().replaceAll("%cop%", frisker.getName())
                        .replaceAll("%time%", String.valueOf(gracePeriod))));
                frisker.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(config
                        .getNode("frisk-request-cop-msg").getString().replaceAll("%player%", frisked.getName())
                        .replaceAll("%time%", String.valueOf(gracePeriod))));

                // Make sure the plugin knows the player needs to respond in some way
                BeatdownData.setWaiting(frisked.getUniqueId(), true);

                Vector3d friskedLoc = frisked.getLocation().getPosition();

                // Checking to see if the player is running away from the cop
                Task.builder().execute(task -> {

                    // If the player has run outside of the radius specified in the config file...
                    if (frisked.getLocation().getPosition().distance(friskedLoc) >= config.getNode("beatdown-radius").getDouble()) {

                        // The player is now officially on the run
                        BeatdownData.setRunning(frisked.getUniqueId(), frisker.getUniqueId(), true);

                        // The time (in minutes) that must pass before a player can be considered "safe" (i.e. no longer on the run)
                        int timeLimit = config.getNode("beatdown-time-limit").getInt();

                        frisked.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(config
                                .getNode("on-the-run-player-msg").getString().replaceAll("%time%",
                                        String.valueOf(timeLimit))));
                        frisker.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(config
                                .getNode("on-the-run-cop-msg").getString().replaceAll("%time%", String.valueOf(timeLimit))
                                .replaceAll("%player%", frisked.getName())));

                        // The task to act as a timer to detect if enough time has passed for them to become safe again.
                        // Task not run on a delay because that would allow for another beatdown attempt on the same player
                        // to be prematurely cancelled (especially becomes an issue when auto-jailing is disabled)
                        Task.builder().execute(new Consumer<Task>() {

                            // Gets the time when this task was initially created
                            long startTime = System.currentTimeMillis();

                            @Override
                            public void accept(Task task) {

                                // The amount of time (in seconds) elapsed since the timer started
                                long timeElapsed = (System.currentTimeMillis() - startTime) / 1000;

                                // Only run while the player is on the run
                                if (BeatdownData.isRunning(frisked.getUniqueId())) {

                                    // If the time limit has been surpassed...
                                    if (timeElapsed >= timeLimit * 60) {

                                        frisked.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(config
                                                .getNode("safe-player-msg").getString()));
                                        frisker.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(config
                                                .getNode("safe-cop-msg").getString().replaceAll("%player%", frisked.getName())));

                                        // The player is now safe
                                        BeatdownData.setRunning(frisked.getUniqueId(), null, false);

                                        // Cancel the timer
                                        task.cancel();

                                    }

                                // If the player is not on the run (i.e. they were caught), cancel the timer
                                } else {

                                    task.cancel();

                                }

                            }

                        // This task will be executed every second
                        }).intervalTicks(20).submit(plugin);

                        // Since the player is now on the run, this task doesn't need to continue running
                        task.cancel();

                    // If the plugin is no longer awaiting a response from the player (see next task)...
                    } else if (!BeatdownData.awaitingResponseFrom(frisked.getUniqueId())) {

                        task.cancel();

                    }

                // This task will be executed every half a second
                }).intervalTicks(10).submit(plugin);

                // If the previous task hasn't detected the player running in the time specified by the config,
                // this task will run and set the player's waiting status to false, causing the previous task to cancel
                // itself. The plugin will assume that the player has complied, and frisking will continue as normal.
                Task.builder().execute(task -> {

                    // If the player is not on the run...
                    if (!BeatdownData.isRunning(frisked.getUniqueId())) {

                        BeatdownData.setWaiting(frisked.getUniqueId(), false);
                        FriskUtil.friskPlayer(frisker, frisked, plugin);

                    }

                }).delay(gracePeriod, TimeUnit.SECONDS).submit(plugin);

            // If beatdown mode is disabled, just frisk the player
            } else {

                FriskUtil.friskPlayer(frisker, frisked, plugin);

            }

        }

    }

}
