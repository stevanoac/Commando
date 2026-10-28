package dev.stevanoac.commando.argument;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable wrapper class providing contextual command execution information.
 */
public class CommandArgs {

    private final CommandSender sender;
    private final Player player;
    private final String label;
    private final String[] args;

    public CommandArgs(CommandSender sender, String label, String[] args) {
        this(sender, (sender instanceof Player) ? (Player) sender : null, label, args);
    }

    public CommandArgs(CommandSender sender, Player player, String label, String[] args) {
        this.sender = Objects.requireNonNull(sender, "CommandSender cannot be null");
        this.player = player;
        this.label = Objects.requireNonNull(label, "Label cannot be null");
        this.args = (args != null) ? args.clone() : new String[0];
    }

    /**
     * Gets the command sender (Console or Player).
     *
     * @return the command sender
     */
    public CommandSender getSender() {
        return sender;
    }

    /**
     * Gets the Player executing this command, or null if executed by console/non-player.
     *
     * @return the Player, or null
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Checks if the sender is a Player.
     *
     * @return true if sender is a Player
     */
    public boolean isPlayer() {
        return player != null;
    }

    /**
     * Checks if the sender is console / non-player.
     *
     * @return true if sender is not a Player
     */
    public boolean isConsole() {
        return player == null;
    }

    /**
     * Gets the command label used to execute the command.
     *
     * @return command label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Gets the arguments passed to this (sub)command.
     *
     * @return defensive copy of argument array
     */
    public String[] getArgs() {
        return args.clone();
    }

    /**
     * Gets the number of arguments.
     *
     * @return argument count
     */
    public int length() {
        return args.length;
    }

    /**
     * Checks if any arguments were provided.
     *
     * @return true if args.length > 0
     */
    public boolean hasArgs() {
        return args.length > 0;
    }

    /**
     * Safely retrieves an argument at the given index.
     *
     * @param index argument index
     * @return argument string, or null if out of bounds
     */
    public String getArg(int index) {
        if (index >= 0 && index < args.length) {
            return args[index];
        }
        return null;
    }

    /**
     * Safely retrieves an argument at the given index, returning a fallback if out of bounds.
     *
     * @param index argument index
     * @param fallback default value if index is out of bounds
     * @return argument string or fallback
     */
    public String getArg(int index, String fallback) {
        String val = getArg(index);
        return (val != null) ? val : fallback;
    }

    /**
     * Joins arguments starting from the given index using a single space.
     *
     * @param startIndex starting argument index
     * @return joined string, or empty string if index out of bounds
     */
    public String joinArgs(int startIndex) {
        return joinArgs(startIndex, " ");
    }

    /**
     * Joins arguments starting from the given index using the given delimiter.
     *
     * @param startIndex starting argument index
     * @param delimiter string delimiter
     * @return joined string, or empty string if index out of bounds
     */
    public String joinArgs(int startIndex, String delimiter) {
        if (startIndex < 0 || startIndex >= args.length) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (builder.length() > 0) {
                builder.append(delimiter);
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }

    /**
     * Checks if the sender has the specified permission node.
     *
     * @param permission permission node
     * @return true if sender has permission
     */
    public boolean hasPermission(String permission) {
        return permission == null || permission.isEmpty() || sender.hasPermission(permission);
    }

    /**
     * Sends a color-formatted message to the command sender.
     *
     * @param message text message supporting '&' color codes
     */
    public void sendMessage(String message) {
        if (message != null && !message.isEmpty()) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    /**
     * Sends multiple color-formatted messages to the command sender.
     *
     * @param messages text messages supporting '&' color codes
     */
    public void sendMessage(String... messages) {
        if (messages != null) {
            for (String message : messages) {
                sendMessage(message);
            }
        }
    }

    @Override
    public String toString() {
        return "CommandArgs{" +
                "sender=" + sender.getName() +
                ", isPlayer=" + isPlayer() +
                ", label='" + label + '\'' +
                ", args=" + Arrays.toString(args) +
                '}';
    }
}
