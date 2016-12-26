package io.github.dchaosknight.friskstick.listeners;

import io.github.dchaosknight.friskstick.FriskStick;
import io.github.dchaosknight.friskstick.data.BeatdownData;
import io.github.dchaosknight.friskstick.util.FriskUtil;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.manipulator.mutable.entity.HealthData;
import org.spongepowered.api.data.value.mutable.MutableBoundedValue;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.text.serializer.TextSerializers;

public class BeatdownListener {

    private FriskStick plugin;

    public BeatdownListener(FriskStick plugin) {

        this.plugin = plugin;

    }

    @Listener
    public void onDamage(DamageEntityEvent event, @Getter("getTargetEntity") Player player) {

        // If the player isn't on the run, don't do anything
        if (!BeatdownData.isRunning(player.getUniqueId())) {

            return;

        }

        // Keeping track of remaining health

        Integer remainingHealth;

        try {

            remainingHealth = BeatdownData.getRemainingHealth(player.getUniqueId());

        } catch (NullPointerException e) {

            remainingHealth = plugin.getConfig().getNode("beatdown-health-loss").getInt();

        }

        remainingHealth -= (int)event.getFinalDamage();

        BeatdownData.setRemainingHealth(player.getUniqueId(), remainingHealth);

        player.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(plugin.getConfig().getNode("damage-msg").getString()
            .replaceAll("%health%", String.valueOf(remainingHealth / 2.0))));

        // If the player has run out of health, frisk them
        if (remainingHealth <= 0) {

            FriskUtil.friskPlayer(Sponge.getServer().getPlayer(BeatdownData.getCopChasingPlayer(player.getUniqueId())).get(), player, plugin);
            BeatdownData.setRunning(player.getUniqueId(), null, false);

        }

        // Cancel the damage
        event.setBaseDamage(0);

    }

}
