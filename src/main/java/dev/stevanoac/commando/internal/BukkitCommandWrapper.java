package dev.stevanoac.commando.internal;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Objects;

/**
 * Dynamic Bukkit Command wrapper registered directly into Bukkit's internal CommandMap.
 */
public class BukkitCommandWrapper extends Command implements PluginIdentifiableCommand {

    private final Plugin plugin;
    private final CommandTree tree;

    public BukkitCommandWrapper(Plugin plugin, CommandTree tree) {
        super(tree.getRootName(), tree.getDescription(), tree.getUsage(), tree.getAliases());
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.tree = Objects.requireNonNull(tree, "CommandTree cannot be null");
    }

    @Override
    public Plugin getPlugin() {
        return plugin;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        return tree.execute(sender, commandLabel, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        return tree.tabComplete(sender, alias, args);
    }

    public CommandTree getTree() {
        return tree;
    }
}
