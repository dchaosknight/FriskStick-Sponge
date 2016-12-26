package io.github.dchaosknight.friskstick.data;

import java.util.*;

/**
 * The central class for data pertaining to beatdown mode.
 */
public class BeatdownData {

    // Contains the players still needing to respond to a frisk request
    private static List<UUID> awaitingResponse = new ArrayList<>();

    // Contains the players actively on the run
    private static List<UUID> playersOnRun = new ArrayList<>();

    // Key: player ID, value: remaining health (for beatdown mode, not their actual health)
    private static Map<UUID, Integer> playerHealth = new HashMap<>();

    // Key: "criminal" ID, value: ID of player chasing the "criminal"
    private static Map<UUID, UUID> playerCopMap = new HashMap<>();

    /**
     * Checks to see if the plugin is waiting for a player to respond to a frisk request.
     *
     * @param playerID The ID of the player being checked
     * @return {@code true} if the player needs to respond, {@code false} otherwise
     */
    public static boolean awaitingResponseFrom(UUID playerID) {

        return awaitingResponse.contains(playerID);

    }

    /**
     * Sets a player's waiting status.
     *
     * @param playerID The ID of the player
     * @param waiting Whether or not the player still has to respond to a frisk request
     */
    public static void setWaiting(UUID playerID, boolean waiting) {

        if (!awaitingResponseFrom(playerID) && waiting) {

            awaitingResponse.add(playerID);

        } else if (awaitingResponseFrom(playerID) && !waiting) {

            awaitingResponse.remove(playerID);

        }

    }

    /**
     * Sets a player's running status. If {@code running} is {@code true} and {@link BeatdownData#awaitingResponseFrom(UUID)}
     * returns {@code true} for this player's {@link UUID}, that player will also be taken off of the waiting list.
     *
     * @param playerID The ID of the player
     * @param copID The ID of the cop chasing the player (can be {@code null} if {@code running} is false)
     * @param running Whether or not the player is on the run
     */
    public static void setRunning(UUID playerID, UUID copID, boolean running) {

        if (running) {

            if (!isRunning(playerID)) {

                playersOnRun.add(playerID);
                playerCopMap.put(playerID, copID);

            }

            if (awaitingResponseFrom(playerID)) {

                setWaiting(playerID, false);

            }

        } else {

            if (isRunning(playerID)) {

                playersOnRun.remove(playerID);
                playerHealth.remove(playerID);
                playerCopMap.remove(playerID);

            }

        }

    }

    /**
     * Checks to see if a player is on the run.
     *
     * @param playerID The ID of the player being checked
     * @return {@code true} if the player is on the run, {@code false} otherwise
     */
    public static boolean isRunning(UUID playerID) {

        return playersOnRun.contains(playerID);

    }

    /**
     * Gets the remaining health for a player on the run. This value does not correspond to the player's actual health
     * value; it is simply a representation of how many more hits the player can take before being frisked.
     *
     * @param playerID The ID of the player
     * @return {@code 0} if the player isn't on the run. Otherwise, the player's remaining health in half hearts or
     * {@code null} if the player's remaining health hasn't been set (see {@link BeatdownData#setRemainingHealth(UUID, int)})
     */
    public static int getRemainingHealth(UUID playerID) {

        if (!isRunning(playerID)) {

            return 0;

        } else {

            return playerHealth.get(playerID);

        }

    }

    /**
     * Sets the remaining health for a player on the run.
     *
     * @param playerID The ID of the player
     * @param newValue The value to which the player's remaining health will be set
     */
    public static void setRemainingHealth(UUID playerID, int newValue) {

        if (!isRunning(playerID)) {

            return;

        }

        playerHealth.put(playerID, newValue);

    }

    /**
     * Gets the cop chasing a player (for use in beatdown mode).
     *
     * @param playerID The player id
     * @return The cop's id or {@code null} if the player isn't on the run
     */
    public static UUID getCopChasingPlayer(UUID playerID) {

        if (isRunning(playerID)) {

            return playerCopMap.get(playerID);

        }

        return null;

    }

}
