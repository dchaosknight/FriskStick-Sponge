package io.github.dchaosknight.friskstick.util;

import io.github.dchaosknight.friskstick.FriskStick;
import ninja.leaping.configurate.ConfigurationNode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class FriskUtil {

    /**
     * Frisks a player.
     *
     * @param frisker The player performing the frisking
     * @param frisked The player being frisked
     */
    public static void friskPlayer(Player frisker, Player frisked, FriskStick plugin) {

        // Get the config
        ConfigurationNode config = plugin.getConfig();

        // Get the list of drugs in the "drugs" node
        List<? extends ConfigurationNode> drugs = config.getNode("drugs").getChildrenList();

        // Contains names for any and all drugs found while frisking
        List<String> drugnames = new ArrayList<>();

        // Iterate through the frisked person's inventory
        frisked.getInventory().slots().forEach(slot -> {

            Optional<ItemStack> item = slot.peek();

            if (item.isPresent()) {

                // Get the id (e.g. "minecraft:wool") and damage value (e.g. "14") of the inventory item
                String itemID = item.get().toContainer().get(DataQuery.of('/', "ItemType")).get().toString();
                String itemDamage = item.get().toContainer().get(DataQuery.of('/', "UnsafeDamage")).get().toString();

                // Iterate through the drug list
                for (ConfigurationNode drug : drugs) {

                    // Get the id of the current drug as an array of two strings:
                    // 1. The actual id (see the comment on itemID)
                    // 2. The damage value
                    String[] configID = drug.getNode("id").getString().trim().split(" ");

                    // Check to see if the inventory and config items match
                    // If statement: if no damage value is specified in the config, every item with the specified id
                    // will be taken
                    // Else-if statement: if a damage value is specified, only items matching both the id and damage value
                    // will be taken
                    if (configID.length == 1) {

                        if (itemID.equalsIgnoreCase(configID[0])) {

                            frisker.getInventory().offer(slot.poll().get());

                            // Only add the name to the list the first time it's found in the inventory
                            if (!drugnames.contains(drug.getNode("name").getString())) {

                                drugnames.add(drug.getNode("name").getString());

                            }

                        }

                    } else if (itemID.equalsIgnoreCase(configID[0]) && itemDamage.equalsIgnoreCase(configID[1])) {

                        frisker.getInventory().offer(slot.poll().get());

                        if (!drugnames.contains(drug.getNode("name").getString())) {

                            drugnames.add(drug.getNode("name").getString());

                        }

                    }

                }

            }

        });

        // If a drug was found...
        if (drugnames.size() > 0) {

            StringBuilder drugList = new StringBuilder();

            for (String name : drugnames) {

                if (drugnames.size() == 2) {

                    // If the current element is the first element in a list of two, add " and "; otherwise, add nothing
                    drugList.append(name).append((drugnames.indexOf(name) == 0) ? " and " : "");

                } else {

                    // If the current element is before the second to last element, add ", "; if it's the second to
                    // last element, add ", and "; if it's the last element, add nothing
                    drugList.append(name).append((drugnames.indexOf(name) < drugnames.size() - 2) ? ", " :
                            (drugnames.indexOf(name) < drugnames.size() - 1) ? ", and " : "");

                }

            }

            // Send corresponding messages to the players
            frisker.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(config.getNode("frisk-success-cop-msg")
                    .getString().replaceAll("%player%", frisked.getName()).replaceAll("%druglist%", drugList.toString())));

            frisked.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(config.getNode("frisk-success-player-msg")
                    .getString().replaceAll("%cop%", frisker.getName()).replaceAll("%druglist%", drugList.toString())));

            // Auto-jailing
            if (config.getNode("enable-auto-jailing").getBoolean()) {

                // Get the list of jail names
                List<String> jails = config.getNode("jail-list").getList(name -> (String) name);

                // Generate a random index from the list of jails
                int jailIndex = new Random().nextInt() % jails.size();

                // Run the jail command
                plugin.getGame().getCommandManager().process(Sponge.getServer().getConsole(), config
                        .getNode("jail-command").getString().replaceAll("%player%", frisked.getName())
                        .replaceAll("%jail%", jails.get(jailIndex)));

            }

            // If no drugs were found...
        } else {

            // Send corresponding messages to the players
            frisker.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(config
                    .getNode("frisk-failure-cop-msg").getString().replaceAll("%player%", frisked.getName())));

            frisked.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(config
                    .getNode("frisk-failure-player-msg").getString().replaceAll("%cop%", frisker.getName())));

        }

    }

}
