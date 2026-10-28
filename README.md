# Commando

A lightweight, annotation-based command framework for Spigot and Paper. Built to replace massive `if (args[0].equalsIgnoreCase(...))` ladders and messy `plugin.yml` setups with clean routing, native type parsing, and proper dependency injection.

Compatible with everything from legacy **1.8.8** up to modern **1.21+**.

---

## Why Commando?

Most older Spigot command frameworks force you to either write zero-arg constructors (leading to `MyPlugin.getInstance()` everywhere) or create separate classes for every subcommand. Commando takes a cleaner approach:

- **Constructor Injection:** Pass your plugin instances, managers, or database clients straight into command classes.
- **No `plugin.yml` Boilerplate:** Commands are injected directly into Bukkit's internal `CommandMap` at runtime.
- **Path-Based Routing:** Define root commands and subcommands using simple paths like `@Command(name = "arena delete")`.
- **Custom Argument Adapters:** Pass objects like `Arena`, `Kit`, or `Player` directly into your method parameters. Commando parses them and suggests them via tab-completion automatically.
- **Built-In Session Cooldowns:** Add quick rate limits (`cooldown = 3`) without writing cooldown maps or task timers.
- **Automatic Paginated Help:** Generate clean, permission-checked `/<command> help [page]` menus automatically.

---

## Setup

Add Commando to your `pom.xml`:

```xml
<dependency>
    <groupId>dev.stevanoac</groupId>
    <artifactId>commando</artifactId>
    <version>1.0.0</version>
    <scope>compile</scope>
</dependency>
```

If you're shading it into your final jar, make sure to relocate it in the `maven-shade-plugin` so it doesn't collide with other plugins using it:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.0</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
            <configuration>
                <relocations>
                    <relocation>
                        <pattern>dev.stevanoac.commando</pattern>
                        <shadedPattern>com.yourplugin.libs.commando</shadedPattern>
                    </relocation>
                </relocations>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## Usage

### 1. Initialize & Register

Set up Commando in your plugin's `onEnable()`. Because you register instances rather than classes, you can pass whatever dependencies your command needs:

```java
public class MyPlugin extends JavaPlugin {

    private Commando commando;

    @Override
    public void onEnable() {
        ArenaManager arenaManager = new ArenaManager(this);

        this.commando = new Commando(this);

        // Register custom adapters
        this.commando.registerAdapter(Arena.class, new ArenaAdapter(arenaManager));

        // Register commands with injected dependencies
        this.commando.register(new ArenaCommand(this, arenaManager));
    }
}
```

### 2. Creating Commands

Methods annotate their route directly in `@Command`. Commands are player-only by default; set `console = true` if you want terminal execution allowed.

```java
public class ArenaCommand {

    private final MyPlugin plugin;
    private final ArenaManager arenaManager;

    public ArenaCommand(MyPlugin plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }

    // Default route: /arena
    @Command(name = "arena", description = "Base arena command")
    public void onRoot(Player player) {
        player.sendMessage("§eType §6/arena help §efor a list of subcommands.");
    }

    // Subcommand: /arena create <name>
    @Command(name = "arena create", permission = "arena.admin", description = "Create an arena")
    public void onCreate(Player player, String name) {
        arenaManager.createArena(name);
        player.sendMessage("§aArena " + name + " created.");
    }

    // Subcommand: /arena delete <arena> (Parses custom Arena object automatically)
    // Cooldown is set to 5 seconds
    @Command(name = "arena delete", permission = "arena.admin", cooldown = 5, console = true, description = "Delete an arena")
    public void onDelete(CommandSender sender, Arena arena) {
        arenaManager.deleteArena(arena);
        sender.sendMessage("§aDeleted arena: " + arena.getName());
    }
}
```

### 3. Argument Adapters (Parsing & Auto Tab-Complete)

Adapters handle two things: turning raw string input into real Java objects, and supplying suggestions for tab completion.

```java
public class ArenaAdapter implements ArgumentAdapter<Arena> {

    private final ArenaManager arenaManager;

    public ArenaAdapter(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    @Override
    public Arena parse(CommandSender sender, String input) throws IllegalArgumentException {
        Arena arena = arenaManager.getArena(input);
        if (arena == null) {
            throw new IllegalArgumentException("No arena found with the name '" + input + "'.");
        }
        return arena;
    }

    @Override
    public List<String> suggest(CommandSender sender, String currentInput) {
        return arenaManager.getArenaNames().stream()
                .filter(name -> name.toLowerCase().startsWith(currentInput.toLowerCase()))
                .collect(Collectors.toList());
    }
}
```

Whenever a method expects an `Arena` parameter, Commando runs this adapter to resolve the argument. If the player types an invalid name, it catches the exception and sends them the error message automatically.

### 4. Paginated Help

Add `@Help` to automatically generate a `/<command> help [page]` route for any root command. Commando filters the list so players only see commands they actually have permission to run.

```java
public class ArenaCommand {

    // Handles: /arena help [page]
    @Help(command = "arena", entriesPerPage = 5)
    public void onHelp(CommandSender sender, HelpPaginator help) {
        help.send(sender);
    }
}
```

---

## Annotation Quick Reference

### `@Command`

| Property | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `name` | `String` | *Required* | Path route (e.g. `"fly"` or `"arena setspawn"`). |
| `aliases` | `String[]` | `{}` | Command aliases. |
| `permission` | `String` | `""` | Bukkit permission required to run. |
| `description` | `String` | `""` | Description displayed in help menus. |
| `console` | `boolean` | `false` | Whether console can execute the command. |
| `cooldown` | `long` | `0` | Rate-limit in seconds (tracked in-memory per UUID). |
| `cooldownMessage` | `String` | Default | Error string displayed on cooldown (supports `%time%`). |

### `@Help`

| Property | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `command` | `String` | *Required* | Target root command name (e.g. `"arena"`). |
| `entriesPerPage` | `int` | `6` | Number of entries rendered per page. |

---

## License

MIT
