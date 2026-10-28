package dev.stevanoac.commando;

import dev.stevanoac.commando.adapter.ArgumentAdapter;
import dev.stevanoac.commando.annotation.Command;
import dev.stevanoac.commando.annotation.Help;
import dev.stevanoac.commando.help.HelpPaginator;
import dev.stevanoac.commando.internal.BukkitCommandWrapper;
import org.bukkit.Server;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class CommandFrameworkTest {

    public interface ServerWithCommandMap extends Server {
        CommandMap getCommandMap();
    }

    private Plugin mockPlugin;
    private ServerWithCommandMap mockServer;
    private SimpleCommandMap commandMap;
    private PluginManager mockPluginManager;
    private CommandFramework framework;

    // Domain models to verify custom Type Adapter and Dependency Injection
    public static class Arena {
        private final String name;
        public Arena(String name) { this.name = name; }
        public String getName() { return name; }
    }

    public static class ArenaManager {
        private final Map<String, Arena> arenas = new HashMap<>();

        public void addArena(String name) { arenas.put(name.toLowerCase(), new Arena(name)); }
        public Arena getArena(String name) { return arenas.get(name.toLowerCase()); }
        public boolean deleteArena(String name) { return arenas.remove(name.toLowerCase()) != null; }
        public List<String> getArenaNames() { return new ArrayList<>(arenas.keySet()); }
    }

    public static class ArenaAdapter implements ArgumentAdapter<Arena> {
        private final ArenaManager arenaManager;

        public ArenaAdapter(ArenaManager arenaManager) {
            this.arenaManager = arenaManager;
        }

        @Override
        public Arena parse(CommandSender sender, String input) throws IllegalArgumentException {
            Arena arena = arenaManager.getArena(input);
            if (arena == null) {
                throw new IllegalArgumentException("Arena '" + input + "' does not exist.");
            }
            return arena;
        }

        @Override
        public List<String> suggest(CommandSender sender, String currentInput) {
            List<String> list = new ArrayList<>();
            for (String name : arenaManager.getArenaNames()) {
                if (name.startsWith(currentInput.toLowerCase())) {
                    list.add(name);
                }
            }
            return list;
        }
    }

    @Command(name = "arena", aliases = {"a"}, permission = "arena.use", console = false)
    public static class ArenaCommands {
        private final ArenaManager arenaManager;
        private Arena lastDeleted = null;
        private String lastCreated = null;

        public ArenaCommands(ArenaManager arenaManager) {
            this.arenaManager = arenaManager;
        }

        @Command(name = "create", permission = "arena.admin", description = "Creates a new arena")
        public void onCreate(Player player, String name) {
            this.lastCreated = name;
            arenaManager.addArena(name);
        }

        @Command(name = "delete", permission = "arena.admin", cooldown = 5, description = "Deletes an arena")
        public void onDelete(Player player, Arena arena) {
            this.lastDeleted = arena;
            arenaManager.deleteArena(arena.getName());
        }

        @Command(name = "list", description = "Lists all registered arenas")
        public void onList(CommandSender sender) {
            sender.sendMessage("Arenas: " + arenaManager.getArenaNames());
        }

        @Command(name = "extra1", description = "Extra route 1")
        public void onExtra1(CommandSender sender) {}

        @Command(name = "extra2", description = "Extra route 2")
        public void onExtra2(CommandSender sender) {}

        @Command(name = "extra3", description = "Extra route 3")
        public void onExtra3(CommandSender sender) {}

        @Command(name = "extra4", description = "Extra route 4")
        public void onExtra4(CommandSender sender) {}
    }

    private ArenaManager arenaManager;
    private ArenaCommands arenaCommands;

    @Before
    public void setUp() {
        mockPlugin = Mockito.mock(Plugin.class);
        mockServer = Mockito.mock(ServerWithCommandMap.class);
        mockPluginManager = Mockito.mock(PluginManager.class);
        commandMap = new SimpleCommandMap(mockServer);

        Mockito.when(mockPlugin.getName()).thenReturn("CommandoTest");
        Mockito.when(mockPlugin.getServer()).thenReturn(mockServer);
        Mockito.when(mockPlugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        Mockito.when(mockServer.getPluginManager()).thenReturn(mockPluginManager);
        Mockito.when(mockServer.getCommandMap()).thenReturn(commandMap);

        framework = new CommandFramework(mockPlugin);

        arenaManager = new ArenaManager();
        arenaManager.addArena("volcano");
        arenaManager.addArena("desert");
        arenaManager.addArena("glacier");

        framework.registerAdapter(Arena.class, new ArenaAdapter(arenaManager));

        arenaCommands = new ArenaCommands(arenaManager);
        framework.register(arenaCommands);
    }

    @Test
    public void testRegistrationAndTreeStructure() {
        BukkitCommandWrapper wrapper = framework.getRegisteredWrappers().get("arena");
        assertNotNull("Root command 'arena' should be registered", wrapper);
        assertEquals("arena", wrapper.getName());
        assertTrue("Alias 'a' should be registered", wrapper.getAliases().contains("a"));
    }

    @Test
    public void testTypedParameterResolution() {
        BukkitCommandWrapper wrapper = framework.getRegisteredWrappers().get("arena");
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        Mockito.when(player.getName()).thenReturn("Steve");
        Mockito.when(player.hasPermission(Mockito.anyString())).thenReturn(true);

        // Execute /arena delete volcano -> ArenaAdapter should parse "volcano" into Arena object
        boolean result = wrapper.execute(player, "arena", new String[]{"delete", "volcano"});
        assertTrue(result);
        assertNotNull("Arena object should be injected into onDelete", arenaCommands.lastDeleted);
        assertEquals("volcano", arenaCommands.lastDeleted.getName());
    }

    @Test
    public void testMissingArgumentShowsUsage() {
        BukkitCommandWrapper wrapper = framework.getRegisteredWrappers().get("arena");
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        Mockito.when(player.getName()).thenReturn("Steve");
        Mockito.when(player.hasPermission(Mockito.anyString())).thenReturn(true);

        // Execute /arena delete without required argument
        wrapper.execute(player, "arena", new String[]{"delete"});
        assertNull("onDelete should not have executed", arenaCommands.lastDeleted);
        Mockito.verify(player).sendMessage(Mockito.contains("Missing required argument"));
        Mockito.verify(player).sendMessage(Mockito.contains("Usage:"));
    }

    @Test
    public void testInvalidAdapterInputShowsError() {
        BukkitCommandWrapper wrapper = framework.getRegisteredWrappers().get("arena");
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        Mockito.when(player.getName()).thenReturn("Steve");
        Mockito.when(player.hasPermission(Mockito.anyString())).thenReturn(true);

        // Execute /arena delete nonExistentArena
        wrapper.execute(player, "arena", new String[]{"delete", "nonExistentArena"});
        assertNull("onDelete should not have executed", arenaCommands.lastDeleted);
        Mockito.verify(player).sendMessage(Mockito.contains("does not exist"));
    }

    @Test
    public void testTabCompletionWithAdapters() {
        BukkitCommandWrapper wrapper = framework.getRegisteredWrappers().get("arena");
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.hasPermission(Mockito.anyString())).thenReturn(true);

        // 1. Subcommand completion on args[0]: /arena d
        List<String> subCompletions = wrapper.tabComplete(player, "arena", new String[]{"d"});
        assertTrue("Subcommands starting with 'd' should suggest 'delete'", subCompletions.contains("delete"));

        // 2. Adapter-based parameter completion on args[1]: /arena delete v
        List<String> arenaCompletions = wrapper.tabComplete(player, "arena", new String[]{"delete", "v"});
        assertEquals("ArenaAdapter should suggest 'volcano'", 1, arenaCompletions.size());
        assertEquals("volcano", arenaCompletions.get(0));
    }

    @Test
    public void testPaginatedHelp() {
        BukkitCommandWrapper wrapper = framework.getRegisteredWrappers().get("arena");
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.hasPermission(Mockito.anyString())).thenReturn(true);

        // Execute /arena help 1
        wrapper.execute(player, "arena", new String[]{"help", "1"});
        Mockito.verify(player).sendMessage(Mockito.contains("ARENA Help (1/2)"));

        // Execute /arena help 2
        wrapper.execute(player, "arena", new String[]{"help", "2"});
        Mockito.verify(player).sendMessage(Mockito.contains("ARENA Help (2/2)"));
    }

    @Test
    public void testCooldownEnforcement() {
        BukkitCommandWrapper wrapper = framework.getRegisteredWrappers().get("arena");
        Player player = Mockito.mock(Player.class);
        UUID uuid = UUID.randomUUID();
        Mockito.when(player.getUniqueId()).thenReturn(uuid);
        Mockito.when(player.getName()).thenReturn("Steve");
        Mockito.when(player.hasPermission(Mockito.anyString())).thenReturn(true);

        // First run
        wrapper.execute(player, "arena", new String[]{"delete", "desert"});
        assertTrue(framework.getCooldownManager().isOnCooldown(uuid, "arena:delete"));

        // Second run immediately blocked
        wrapper.execute(player, "arena", new String[]{"delete", "desert"});
        Mockito.verify(player).sendMessage(Mockito.contains("Please wait"));
    }

    @Test
    public void testConsoleBlockedWhenPlayerDeclared() {
        BukkitCommandWrapper wrapper = framework.getRegisteredWrappers().get("arena");
        ConsoleCommandSender console = Mockito.mock(ConsoleCommandSender.class);
        Mockito.when(console.hasPermission(Mockito.anyString())).thenReturn(true);

        // Console executes player-only command (/arena create)
        wrapper.execute(console, "arena", new String[]{"create", "ocean"});
        Mockito.verify(console).sendMessage(Mockito.contains("only be executed by players"));
    }
}
