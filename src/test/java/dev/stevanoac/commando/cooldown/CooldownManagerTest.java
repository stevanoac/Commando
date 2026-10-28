package dev.stevanoac.commando.cooldown;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.Assert.*;

public class CooldownManagerTest {

    private CooldownManager cooldownManager;
    private UUID playerUuid;

    @Before
    public void setUp() {
        cooldownManager = new CooldownManager();
        playerUuid = UUID.randomUUID();
    }

    @Test
    public void testCooldownLifecycle() {
        assertFalse("New player should not be on cooldown", cooldownManager.isOnCooldown(playerUuid, "test"));

        // Apply 5 second cooldown
        cooldownManager.applyCooldown(playerUuid, "test", 5);

        assertTrue("Player should be on cooldown", cooldownManager.isOnCooldown(playerUuid, "test"));
        assertTrue("Remaining seconds should be <= 5", cooldownManager.getRemainingSeconds(playerUuid, "test") <= 5);
        assertTrue("Remaining seconds should be > 0", cooldownManager.getRemainingSeconds(playerUuid, "test") > 0);

        // Remove cooldown
        cooldownManager.removeCooldown(playerUuid, "test");
        assertFalse("Player should no longer be on cooldown", cooldownManager.isOnCooldown(playerUuid, "test"));
    }

    @Test
    public void testPlayerQuitClearsCooldown() {
        cooldownManager.applyCooldown(playerUuid, "arena", 10);
        assertTrue(cooldownManager.isOnCooldown(playerUuid, "arena"));

        Player mockPlayer = Mockito.mock(Player.class);
        Mockito.when(mockPlayer.getUniqueId()).thenReturn(playerUuid);

        PlayerQuitEvent quitEvent = new PlayerQuitEvent(mockPlayer, "Quit message");
        cooldownManager.onPlayerQuit(quitEvent);

        assertFalse("Cooldown should be removed upon player quit", cooldownManager.isOnCooldown(playerUuid, "arena"));
    }
}
