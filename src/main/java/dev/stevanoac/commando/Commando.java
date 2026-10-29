package dev.stevanoac.commando;

import dev.stevanoac.commando.adapter.*;
import dev.stevanoac.commando.annotation.Command;
import dev.stevanoac.commando.annotation.Default;
import dev.stevanoac.commando.annotation.Help;
import dev.stevanoac.commando.cooldown.CooldownManager;
import dev.stevanoac.commando.help.HelpFormat;
import dev.stevanoac.commando.internal.BukkitCommandWrapper;
import dev.stevanoac.commando.internal.CommandTree;
import dev.stevanoac.commando.internal.HelpHandler;
import org.bukkit.Server;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Modern, annotation-driven Spigot Command Framework featuring path-based tree routing,
 * custom argument adapters, universal tab-completion, and paginated help.
 */
public class Commando {

    private final Plugin plugin;
    private final CooldownManager cooldownManager;
    private final CommandMap commandMap;
    private final Map<String, org.bukkit.command.Command> knownCommands;

    private final Map<Class<?>, ArgumentAdapter<?>> adapters = new HashMap<>();
    private final Map<String, CommandTree> trees = new HashMap<>();
    private final Map<String, BukkitCommandWrapper> registeredWrappers = new HashMap<>();

    private String playerOnlyMessage = "&cThis command can only be executed by players.";
    private String noPermissionMessage = "&cYou do not have permission to execute this command.";
    private String cooldownMessage = "&cPlease wait {seconds}s before using this command again.";
    private HelpFormat helpFormat = HelpFormat.defaultFormat();

    public Commando(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.cooldownManager = new CooldownManager();

        // Register quit listener for memory leak prevention
        this.plugin.getServer().getPluginManager().registerEvents(this.cooldownManager, this.plugin);

        // Reflectively obtain Bukkit's internal CommandMap
        this.commandMap = resolveCommandMap();
        this.knownCommands = resolveKnownCommands(this.commandMap);

        // Register built-in type adapters
        registerDefaultAdapters();
    }

    private void registerDefaultAdapters() {
        registerAdapter(String.class, new StringAdapter());
        registerAdapter(Integer.class, new IntegerAdapter());
        registerAdapter(int.class, new IntegerAdapter());
        registerAdapter(Double.class, new DoubleAdapter());
        registerAdapter(double.class, new DoubleAdapter());
        registerAdapter(Boolean.class, new BooleanAdapter());
        registerAdapter(boolean.class, new BooleanAdapter());
        registerAdapter(Player.class, new PlayerAdapter());
    }

    /**
     * Registers a custom argument adapter for typed parameter injection and tab-completion.
     *
     * @param type target class type
     * @param adapter argument adapter implementation
     * @param <T> adapter type
     * @return this framework instance for fluent chaining
     */
    public <T> Commando registerAdapter(Class<T> type, ArgumentAdapter<T> adapter) {
        Objects.requireNonNull(type, "Type cannot be null");
        Objects.requireNonNull(adapter, "Adapter cannot be null");
        adapters.put(type, adapter);
        return this;
    }

    /**
     * Retrieves the argument adapter registered for the given class type.
     *
     * @param type target class type
     * @return argument adapter or null if not found
     */
    public ArgumentAdapter<?> getAdapter(Class<?> type) {
        if (type == null) {
            return null;
        }

        ArgumentAdapter<?> direct = adapters.get(type);
        if (direct != null) {
            return direct;
        }

        // Check primitive / wrapper fallbacks
        if (type.equals(int.class)) return adapters.get(Integer.class);
        if (type.equals(Integer.class)) return adapters.get(int.class);
        if (type.equals(double.class)) return adapters.get(Double.class);
        if (type.equals(Double.class)) return adapters.get(double.class);
        if (type.equals(boolean.class)) return adapters.get(Boolean.class);
        if (type.equals(Boolean.class)) return adapters.get(boolean.class);

        // Check polymorphic inheritance
        for (Map.Entry<Class<?>, ArgumentAdapter<?>> entry : adapters.entrySet()) {
            if (entry.getKey().isAssignableFrom(type)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Registers one or more pre-instantiated command handler objects.
     *
     * @param commandInstances array of command handler objects
     */
    public void register(Object... commandInstances) {
        if (commandInstances == null) {
            return;
        }
        for (Object instance : commandInstances) {
            register(instance);
        }
    }

    /**
     * Registers a pre-instantiated command handler object.
     *
     * @param commandInstance pre-instantiated command handler object
     */
    public void register(Object commandInstance) {
        Objects.requireNonNull(commandInstance, "Command instance cannot be null");

        Class<?> clazz = commandInstance.getClass();
        Command classAnno = clazz.getAnnotation(Command.class);

        String classRoute = null;
        if (classAnno != null) {
            classRoute = !classAnno.name().isEmpty() ? classAnno.name() : classAnno.value();
            if (classRoute.isEmpty()) {
                classRoute = null;
            }
        }

        Set<String> touchedRoots = new HashSet<>();

        // Scan methods
        for (Method method : clazz.getDeclaredMethods()) {
            // @Command on method
            if (method.isAnnotationPresent(Command.class)) {
                Command methodAnno = method.getAnnotation(Command.class);
                String methodRoute = !methodAnno.name().isEmpty() ? methodAnno.name() : methodAnno.value();

                String fullRoute;
                if (classRoute != null) {
                    if (methodRoute.isEmpty()) {
                        fullRoute = classRoute;
                    } else if (methodRoute.equalsIgnoreCase(classRoute) || methodRoute.toLowerCase().startsWith(classRoute.toLowerCase() + " ")) {
                        fullRoute = methodRoute;
                    } else {
                        fullRoute = classRoute + " " + methodRoute;
                    }
                } else {
                    if (methodRoute.isEmpty()) {
                        throw new IllegalArgumentException("Method " + method.getName() + " in " + clazz.getName()
                                + " is missing a route path in @Command.");
                    }
                    fullRoute = methodRoute;
                }

                String rootToken = fullRoute.trim().split("\\s+")[0].toLowerCase();
                CommandTree tree = getOrCreateTree(rootToken, (classAnno != null) ? classAnno : methodAnno);
                tree.registerRoute(fullRoute, methodAnno, method, commandInstance);
                touchedRoots.add(rootToken);
            }

            // @Default on method
            if (method.isAnnotationPresent(Default.class)) {
                if (classRoute == null) {
                    throw new IllegalArgumentException("@Default method " + method.getName() + " in " + clazz.getName()
                            + " requires a class-level @Command annotation to establish the root command context.");
                }
                String rootToken = classRoute.trim().split("\\s+")[0].toLowerCase();
                CommandTree tree = getOrCreateTree(rootToken, classAnno);
                Default defAnno = method.getAnnotation(Default.class);
                tree.registerRoute(classRoute, createSyntheticCommand(defAnno, classAnno), method, commandInstance);
                touchedRoots.add(rootToken);
            }

            // @Help on method
            if (method.isAnnotationPresent(Help.class)) {
                Help helpAnno = method.getAnnotation(Help.class);
                String rootToken = helpAnno.command().trim().split("\\s+")[0].toLowerCase();
                CommandTree tree = getOrCreateTree(rootToken, classAnno);
                tree.setHelpHandler(new HelpHandler(method, commandInstance, helpAnno));
                touchedRoots.add(rootToken);
            }
        }

        // If class-level @Command had no explicit methods, register root route if class had a fallback
        if (touchedRoots.isEmpty() && classRoute != null) {
            String rootToken = classRoute.trim().split("\\s+")[0].toLowerCase();
            getOrCreateTree(rootToken, classAnno);
            touchedRoots.add(rootToken);
        }

        // Bind and register in Bukkit's CommandMap
        for (String root : touchedRoots) {
            CommandTree tree = trees.get(root);
            if (tree != null) {
                bindToBukkit(tree);
            }
        }
    }

    private CommandTree getOrCreateTree(String rootName, Command fallbackAnno) {
        String lower = rootName.toLowerCase();
        CommandTree tree = trees.get(lower);
        if (tree == null) {
            Command anno = fallbackAnno;
            if (anno == null) {
                anno = createEmptyCommand(rootName);
            }
            tree = new CommandTree(this, anno, lower);
            trees.put(lower, tree);
        }
        return tree;
    }

    private void bindToBukkit(CommandTree tree) {
        String root = tree.getRootName();
        unregister(root);

        BukkitCommandWrapper wrapper = new BukkitCommandWrapper(plugin, tree);
        commandMap.register(plugin.getName().toLowerCase(), wrapper);
        registeredWrappers.put(root, wrapper);
    }

    /**
     * Unregisters a command and all its aliases from Bukkit's CommandMap.
     *
     * @param commandName root command name to unregister
     */
    public void unregister(String commandName) {
        if (commandName == null) {
            return;
        }

        String lower = commandName.toLowerCase();
        BukkitCommandWrapper wrapper = registeredWrappers.remove(lower);

        if (knownCommands != null) {
            knownCommands.remove(lower);
            knownCommands.remove(plugin.getName().toLowerCase() + ":" + lower);

            if (wrapper != null) {
                for (String alias : wrapper.getAliases()) {
                    knownCommands.remove(alias.toLowerCase());
                    knownCommands.remove(plugin.getName().toLowerCase() + ":" + alias.toLowerCase());
                }
                wrapper.unregister(commandMap);
            }
        }
    }

    /**
     * Unregisters all commands registered through this framework.
     */
    public void unregisterAll() {
        for (String root : new ArrayList<>(registeredWrappers.keySet())) {
            unregister(root);
        }
        registeredWrappers.clear();
        trees.clear();
        cooldownManager.clearAll();
    }

    private Command createEmptyCommand(final String name) {
        return new Command() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() { return Command.class; }
            @Override
            public String name() { return name; }
            @Override
            public String value() { return name; }
            @Override
            public String[] aliases() { return new String[0]; }
            @Override
            public String permission() { return ""; }
            @Override
            public String permissionMessage() { return ""; }
            @Override
            public String description() { return ""; }
            @Override
            public String usage() { return ""; }
            @Override
            public boolean console() { return false; }
            @Override
            public long cooldown() { return 0; }
        };
    }

    private Command createSyntheticCommand(final Default defAnno, final Command classAnno) {
        return new Command() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() { return Command.class; }
            @Override
            public String name() { return classAnno != null ? classAnno.name() : ""; }
            @Override
            public String value() { return classAnno != null ? classAnno.value() : ""; }
            @Override
            public String[] aliases() { return new String[0]; }
            @Override
            public String permission() {
                return !defAnno.permission().isEmpty() ? defAnno.permission() : (classAnno != null ? classAnno.permission() : "");
            }
            @Override
            public String permissionMessage() {
                return !defAnno.permissionMessage().isEmpty() ? defAnno.permissionMessage() : (classAnno != null ? classAnno.permissionMessage() : "");
            }
            @Override
            public String description() { return classAnno != null ? classAnno.description() : ""; }
            @Override
            public String usage() { return classAnno != null ? classAnno.usage() : ""; }
            @Override
            public boolean console() {
                return defAnno.console() || (classAnno != null && classAnno.console());
            }
            @Override
            public long cooldown() {
                return defAnno.cooldown() > 0 ? defAnno.cooldown() : (classAnno != null ? classAnno.cooldown() : 0);
            }
        };
    }

    private CommandMap resolveCommandMap() {
        Server server = plugin.getServer();

        try {
            Method method = server.getClass().getDeclaredMethod("getCommandMap");
            method.setAccessible(true);
            return (CommandMap) method.invoke(server);
        } catch (Exception ignored) {}

        try {
            Field field = server.getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (CommandMap) field.get(server);
        } catch (Exception ignored) {}

        try {
            PluginManager pm = server.getPluginManager();
            Field field = pm.getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (CommandMap) field.get(pm);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve Bukkit CommandMap via reflection", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, org.bukkit.command.Command> resolveKnownCommands(CommandMap map) {
        if (map == null) return null;
        Class<?> current = map.getClass();
        while (current != null && current != Object.class) {
            try {
                Field field = current.getDeclaredField("knownCommands");
                field.setAccessible(true);
                return (Map<String, org.bukkit.command.Command>) field.get(map);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (Exception e) {
                break;
            }
        }
        return null;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public CommandMap getCommandMap() {
        return commandMap;
    }

    public Map<String, BukkitCommandWrapper> getRegisteredWrappers() {
        return Collections.unmodifiableMap(registeredWrappers);
    }

    public String getPlayerOnlyMessage() {
        return playerOnlyMessage;
    }

    public Commando setPlayerOnlyMessage(String playerOnlyMessage) {
        this.playerOnlyMessage = playerOnlyMessage;
        return this;
    }

    public String getNoPermissionMessage() {
        return noPermissionMessage;
    }

    public Commando setNoPermissionMessage(String noPermissionMessage) {
        this.noPermissionMessage = noPermissionMessage;
        return this;
    }

    public String getCooldownMessage() {
        return cooldownMessage;
    }

    public Commando setCooldownMessage(String cooldownMessage) {
        this.cooldownMessage = cooldownMessage;
        return this;
    }

    private void invalidateAllPaginators() {
        for (CommandTree tree : trees.values()) {
            tree.invalidatePaginator();
        }
    }

    public HelpFormat getHelpFormat() {
        return helpFormat;
    }

    public HelpFormat getPaginatorFormat() {
        return helpFormat;
    }

    public Commando setHelpFormat(HelpFormat helpFormat) {
        this.helpFormat = (helpFormat != null) ? helpFormat : HelpFormat.defaultFormat();
        invalidateAllPaginators();
        return this;
    }

    public Commando setPaginatorFormat(HelpFormat helpFormat) {
        return setHelpFormat(helpFormat);
    }

    public Commando setHelpFormat(String header, String entry, String footer) {
        return setHelpFormat(new HelpFormat(header, entry, footer));
    }

    public Commando setPaginatorFormat(String header, String entry, String footer) {
        return setHelpFormat(header, entry, footer);
    }

    public Commando setHelpFormat(String header, String entry, String footer, String emptyMessage) {
        return setHelpFormat(new HelpFormat(header, entry, footer, emptyMessage));
    }

    public Commando setPaginatorFormat(String header, String entry, String footer, String emptyMessage) {
        return setHelpFormat(header, entry, footer, emptyMessage);
    }

    public Commando setHelpHeaderFormat(String... header) {
        this.helpFormat = this.helpFormat.toBuilder().header(header).build();
        invalidateAllPaginators();
        return this;
    }

    public Commando setPaginatorHeaderFormat(String... header) {
        return setHelpHeaderFormat(header);
    }

    public Commando setHelpEntryFormat(String entry) {
        this.helpFormat = this.helpFormat.toBuilder().entry(entry).build();
        invalidateAllPaginators();
        return this;
    }

    public Commando setPaginatorEntryFormat(String entry) {
        return setHelpEntryFormat(entry);
    }

    public Commando setHelpFooterFormat(String... footer) {
        this.helpFormat = this.helpFormat.toBuilder().footer(footer).build();
        invalidateAllPaginators();
        return this;
    }

    public Commando setPaginatorFooterFormat(String... footer) {
        return setHelpFooterFormat(footer);
    }

    public Commando setHelpEmptyMessage(String... emptyMessage) {
        this.helpFormat = this.helpFormat.toBuilder().emptyMessage(emptyMessage).build();
        invalidateAllPaginators();
        return this;
    }

    public Commando setPaginatorEmptyMessage(String... emptyMessage) {
        return setHelpEmptyMessage(emptyMessage);
    }
}
