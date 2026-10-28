package dev.stevanoac.commando.argument;

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.*;

public class CommandArgsTest {

    @Test
    public void testPlayerArgs() {
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getName()).thenReturn("Steve");

        CommandArgs args = new CommandArgs(player, "arena", new String[]{"join", "volcano"});

        assertTrue(args.isPlayer());
        assertFalse(args.isConsole());
        assertEquals(player, args.getPlayer());
        assertEquals("arena", args.getLabel());
        assertEquals(2, args.length());
        assertEquals("join", args.getArg(0));
        assertEquals("volcano", args.getArg(1));
        assertNull(args.getArg(2));
        assertEquals("join volcano", args.joinArgs(0));
    }

    @Test
    public void testConsoleArgs() {
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        Mockito.when(console.getName()).thenReturn("CONSOLE");

        CommandArgs args = new CommandArgs(console, "arena", new String[]{"reload"});

        assertFalse(args.isPlayer());
        assertTrue(args.isConsole());
        assertNull(args.getPlayer());
        assertEquals("reload", args.getArg(0));
    }
}
