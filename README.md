# Commando

Commando is a modern command library for Spigot and Paper servers (1.8.8 – 1.21+). It handles nested subcommands, tab-completion, type parsing, and paginated help using annotations—without needing to touch `plugin.yml`.

---

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>dev.stevanoac</groupId>
    <artifactId>Commando</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>compile</scope>
</dependency>
```

If you shade Commando into your jar, relocate it in `maven-shade-plugin` so it won't conflict with other plugins:

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
                        <shadedPattern>com.yourname.yourplugin.libs.commando</shadedPattern>
                    </relocation>
                </relocations>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## Quick Start

### 1. Initialize in `onEnable`

Commands are registered as instances, so you can pass whatever dependencies your handlers need directly through constructors:

```java
public class MyPlugin extends JavaPlugin {

    private Commando commando;

    @Override
    public void onEnable() {
        this.commando = new Commando(this);

        // Register command classes
        this.commando.register(new WarpCommand(this));
    }
}
```

### 2. Create a Command Class

Annotate your methods with `@Command`. The first argument can be `Player` (player-only by default) or `CommandSender` (players and console).

```java
@Command(name = "warp", description = "Base warp command")
public class WarpCommand {

    private final MyPlugin plugin;

    public WarpCommand(MyPlugin plugin) {
        this.plugin = plugin;
    }

    // Fallback when a player just types "/warp"
    @Default
    public void onDefault(Player player) {
        player.sendMessage("§eType §6/warp help §efor a list of warps.");
    }

    // Subcommand: /warp to <name>
    @Command(name = "to", permission = "warp.use", description = "Teleport to a warp")
    public void onTeleport(Player player, String warpName) {
        player.sendMessage("§aTeleporting to " + warpName + "...");
    }

    // Subcommand: /warp set <name>
    // Rate-limited to once every 5 seconds
    @Command(name = "set", permission = "warp.admin", cooldown = 5, description = "Create a warp")
    public void onSet(Player player, String warpName) {
        player.sendMessage("§aWarp " + warpName + " created!");
    }

    // Console-friendly command: /warp reload
    @Command(name = "reload", permission = "warp.admin", console = true, description = "Reload warps")
    public void onReload(CommandSender sender) {
        sender.sendMessage("§aWarps reloaded.");
    }
}
```

---

## Key Features

### Built-In Argument Parsers

Method parameters after the sender are parsed automatically. If an argument is missing or invalid, Commando displays a clean usage message.

Supported out of the box:
- `String`
- `int` / `Integer`
- `double` / `Double`
- `boolean` / `Boolean`
- `Player` (auto-completed by online player names)
- `String[]` (captures any trailing varargs)
- `CommandArgs` (wrapper with helper methods like `.joinArgs()` and `.hasPermission()`)

```java
@Command(name = "eco give", permission = "eco.admin", console = true)
public void giveMoney(CommandSender sender, Player target, double amount) {
    sender.sendMessage("Gave $" + amount + " to " + target.getName());
}
```

### Custom Argument Adapters

Define custom argument types so your command methods receive real objects instead of raw strings. Implement `ArgumentAdapter<T>`:

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

Register it once with Commando:

```java
commando.registerAdapter(Arena.class, new ArenaAdapter(arenaManager));
```

Now you can use `Arena` anywhere in your command methods with automatic tab-completion and error handling:

```java
@Command(name = "arena delete", permission = "arena.admin")
public void deleteArena(Player player, Arena arena) {
    arenaManager.delete(arena);
    player.sendMessage("§aArena deleted: " + arena.getName());
}
```

---

### Paginated Help

Commando automatically builds permission-checked help menus for `/<command> help [page]`.

#### Custom Help Formatting

You can customize the layout, colors, and placeholders globally with `setPaginatorFormat`:

```java
commando.setPaginatorFormat(
    "&8------\n&eHelp %page% / %maxPages%",
    "&e/%command% &7- %description%",
    "&8------"
);
```

Or use the builder for complete control over headers, footers, last-page footers, and empty messages:

```java
commando.setHelpFormat(HelpFormat.builder()
    .header("&8&m----------------------------------------", "&eHelp for &6/%COMMAND% &7(&e%page%&7/&e%maxPages%&7)")
    .entry("  &e/%syntax% &7- %description%")
    .footer("&7Type &e/%command% help %nextPage% &7for more.", "&8&m----------------------------------------")
    .lastPageFooter("&7End of help list.", "&8&m----------------------------------------")
    .emptyMessage("&cNo commands available for /%command%.")
    .build()
);
```

#### Help Placeholders

| Placeholder | Where | Description |
| :--- | :--- | :--- |
| `%command%` / `%root%` | Header / Footer / Empty | Root command name (e.g. `warp`) |
| `%COMMAND%` / `%ROOT%` | Header / Footer / Empty | Uppercase root command name (e.g. `WARP`) |
| `%page%` | Header / Footer / Entry | Current page number |
| `%maxPages%` / `%totalPages%` | Header / Footer / Entry | Total page count |
| `%nextPage%` | Header / Footer | Next page number |
| `%prevPage%` | Header / Footer | Previous page number |
| `%totalEntries%` | Header / Footer | Total count of visible commands |
| `%syntax%` / `%command%` | Entry | Full route syntax (e.g. `warp set <name>`) |
| `%description%` | Entry | Command description (cleans up preceding ` - ` when blank) |
| `%permission%` | Entry | Permission node required |

> Both `%placeholder%` and `{placeholder}` syntaxes work, and `&` color codes are converted automatically.

#### Custom `@Help` Methods

If you need custom handling for a specific root command's help route:

```java
@Help(command = "warp", entriesPerPage = 5)
public void onHelp(CommandSender sender, HelpPaginator help, int page) {
    // Optionally customize formatting for this command only
    help.setFormat("&8--- &eWarp Help &8---", "&6/%command% &7- %description%", "&8-----------------");
    help.send(sender, page);
}
```

---

### Customizing Default Messages

You can override standard framework messages during setup:

```java
commando.setPlayerOnlyMessage("&cOnly players can run this command.");
commando.setNoPermissionMessage("&cYou do not have permission to do that.");
commando.setCooldownMessage("&cPlease wait {seconds}s before using this command again.");
```

---

## License

MIT
