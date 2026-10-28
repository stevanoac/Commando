package dev.stevanoac.commando.cooldown;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages short and session command cooldowns in memory using player UUIDs and epoch timestamps.
 */
public class CooldownManager implements Listener {

    /**
     * Map of player UUID -> Map of command key -> expiration epoch timestamp in milliseconds.
     */
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    /**
     * Checks if a player is currently on cooldown for a given command key.
     *
     * @param uuid player unique ID
     * @param commandKey unique identifier for the command or subcommand
     * @return true if still on cooldown, false otherwise
     */
    public boolean isOnCooldown(UUID uuid, String commandKey) {
        if (uuid == null || commandKey == null) {
            return false;
        }

        Map<String, Long> userCooldowns = cooldowns.get(uuid);
        if (userCooldowns == null) {
            return false;
        }

        String normalizedKey = commandKey.toLowerCase();
        Long expiry = userCooldowns.get(normalizedKey);
        if (expiry == null) {
            return false;
        }

        if (System.currentTimeMillis() >= expiry) {
            userCooldowns.remove(normalizedKey);
            if (userCooldowns.isEmpty()) {
                cooldowns.remove(uuid);
            }
            return false;
        }

        return true;
    }

    /**
     * Gets the remaining cooldown time in milliseconds.
     *
     * @param uuid player unique ID
     * @param commandKey unique identifier for the command or subcommand
     * @return remaining milliseconds, or 0 if not on cooldown
     */
    public long getRemainingMillis(UUID uuid, String commandKey) {
        if (uuid == null || commandKey == null) {
            return 0L;
        }

        Map<String, Long> userCooldowns = cooldowns.get(uuid);
        if (userCooldowns == null) {
            return 0L;
        }

        Long expiry = userCooldowns.get(commandKey.toLowerCase());
        if (expiry == null) {
            return 0L;
        }

        long remaining = expiry - System.currentTimeMillis();
        if (remaining <= 0) {
            userCooldowns.remove(commandKey.toLowerCase());
            return 0L;
        }

        return remaining;
    }

    /**
     * Gets the remaining cooldown time rounded up to the nearest whole second.
     *
     * @param uuid player unique ID
     * @param commandKey unique identifier for the command or subcommand
     * @return remaining seconds (e.g. 5)
     */
    public long getRemainingSeconds(UUID uuid, String commandKey) {
        long millis = getRemainingMillis(uuid, commandKey);
        return (millis + 999L) / 1000L;
    }

    /**
     * Gets the remaining cooldown formatted with one decimal place.
     *
     * @param uuid player unique ID
     * @param commandKey unique identifier for the command or subcommand
     * @return remaining seconds with decimal precision (e.g. 2.4)
     */
    public double getRemainingSecondsPrecise(UUID uuid, String commandKey) {
        long millis = getRemainingMillis(uuid, commandKey);
        return Math.max(0.0, Math.ceil(millis / 100.0) / 10.0);
    }

    /**
     * Applies a cooldown to a player for a specific command key.
     *
     * @param uuid player unique ID
     * @param commandKey unique identifier for the command or subcommand
     * @param seconds duration of cooldown in seconds
     */
    public void applyCooldown(UUID uuid, String commandKey, long seconds) {
        if (uuid == null || commandKey == null || seconds <= 0) {
            return;
        }

        long expiryTime = System.currentTimeMillis() + (seconds * 1000L);
        cooldowns.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>())
                .put(commandKey.toLowerCase(), expiryTime);
    }

    /**
     * Removes an active cooldown for a player.
     *
     * @param uuid player unique ID
     * @param commandKey unique identifier for the command or subcommand
     */
    public void removeCooldown(UUID uuid, String commandKey) {
        if (uuid == null || commandKey == null) {
            return;
        }

        Map<String, Long> userCooldowns = cooldowns.get(uuid);
        if (userCooldowns != null) {
            userCooldowns.remove(commandKey.toLowerCase());
        }
    }

    /**
     * Clears all cooldowns for a player.
     *
     * @param uuid player unique ID
     */
    public void clear(UUID uuid) {
        if (uuid != null) {
            cooldowns.remove(uuid);
        }
    }

    /**
     * Clears all cooldowns across all players.
     */
    public void clearAll() {
        cooldowns.clear();
    }

    /**
     * Cleans up cooldown data when a player leaves to prevent memory leaks.
     *
     * @param event PlayerQuitEvent
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        clear(event.getPlayer().getUniqueId());
    }
}
