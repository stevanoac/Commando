package dev.stevanoac.commando.help;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Handles paginated help generation and rendering for command routes.
 */
public class HelpPaginator {

    private final String rootCommand;
    private final int entriesPerPage;
    private final List<HelpEntry> entries;

    public HelpPaginator(String rootCommand, int entriesPerPage, List<HelpEntry> entries) {
        this.rootCommand = rootCommand;
        this.entriesPerPage = (entriesPerPage > 0) ? entriesPerPage : 6;
        this.entries = new ArrayList<>(entries);
        this.entries.sort(Comparator.comparing(HelpEntry::getSyntax));
    }

    /**
     * Filters entries visible to the given sender.
     *
     * @param sender the command sender requesting help
     * @return list of visible help entries
     */
    public List<HelpEntry> getVisibleEntries(CommandSender sender) {
        List<HelpEntry> visible = new ArrayList<>();
        boolean isPlayer = sender instanceof Player;

        for (HelpEntry entry : entries) {
            if (!entry.isConsoleAllowed() && !isPlayer) {
                continue;
            }
            if (!entry.getPermission().isEmpty() && !sender.hasPermission(entry.getPermission())) {
                continue;
            }
            visible.add(entry);
        }
        return visible;
    }

    /**
     * Calculates the total number of pages available for the given sender.
     *
     * @param sender the command sender
     * @return total pages
     */
    public int getTotalPages(CommandSender sender) {
        List<HelpEntry> visible = getVisibleEntries(sender);
        if (visible.isEmpty()) {
            return 1;
        }
        return (int) Math.ceil((double) visible.size() / entriesPerPage);
    }

    /**
     * Renders and sends the requested help page to the command sender.
     *
     * @param sender the command sender
     * @param requestedPage 1-based requested page number
     */
    public void sendHelp(CommandSender sender, int requestedPage) {
        List<HelpEntry> visible = getVisibleEntries(sender);

        if (visible.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No commands available for /" + rootCommand + ".");
            return;
        }

        int totalPages = (int) Math.ceil((double) visible.size() / entriesPerPage);
        if (totalPages <= 0) {
            totalPages = 1;
        }

        int page = requestedPage;
        if (page < 1) {
            page = 1;
        }
        if (page > totalPages) {
            page = totalPages;
        }

        int startIndex = (page - 1) * entriesPerPage;
        int endIndex = Math.min(startIndex + entriesPerPage, visible.size());

        // Header
        sender.sendMessage(ChatColor.YELLOW + "=== " + ChatColor.GOLD + rootCommand.toUpperCase() + " Help ("
                + page + "/" + totalPages + ") " + ChatColor.YELLOW + "===");

        // Entries
        for (int i = startIndex; i < endIndex; i++) {
            HelpEntry entry = visible.get(i);
            String desc = entry.getDescription().isEmpty() ? "" : ChatColor.GRAY + " - " + entry.getDescription();
            sender.sendMessage(ChatColor.GOLD + "/" + entry.getSyntax() + desc);
        }

        // Footer
        if (page < totalPages) {
            sender.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.GOLD + "/" + rootCommand + " help " + (page + 1)
                    + ChatColor.YELLOW + " for more.");
        } else {
            sender.sendMessage(ChatColor.YELLOW + "Use " + ChatColor.GOLD + "/" + rootCommand + " help <page> "
                    + ChatColor.YELLOW + "for more.");
        }
    }

    public String getRootCommand() {
        return rootCommand;
    }

    public int getEntriesPerPage() {
        return entriesPerPage;
    }

    public List<HelpEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }
}
