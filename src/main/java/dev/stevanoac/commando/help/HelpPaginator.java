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
    private HelpFormat format;

    public HelpPaginator(String rootCommand, int entriesPerPage, List<HelpEntry> entries) {
        this(rootCommand, entriesPerPage, entries, HelpFormat.defaultFormat());
    }

    public HelpPaginator(String rootCommand, int entriesPerPage, List<HelpEntry> entries, HelpFormat format) {
        this.rootCommand = rootCommand;
        this.entriesPerPage = (entriesPerPage > 0) ? entriesPerPage : 6;
        this.entries = new ArrayList<>(entries);
        this.entries.sort(Comparator.comparing(HelpEntry::getSyntax));
        this.format = (format != null) ? format : HelpFormat.defaultFormat();
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
     * Convenience method to render and send page 1 of help to the command sender.
     *
     * @param sender the command sender
     */
    public void send(CommandSender sender) {
        sendHelp(sender, 1);
    }

    /**
     * Convenience method to render and send the requested page of help to the command sender.
     *
     * @param sender the command sender
     * @param requestedPage 1-based requested page number
     */
    public void send(CommandSender sender, int requestedPage) {
        sendHelp(sender, requestedPage);
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
            List<String> emptyLines = format.getEmptyMessageLines();
            if (emptyLines != null && !emptyLines.isEmpty()) {
                for (String line : emptyLines) {
                    sender.sendMessage(applyHeaderFooterPlaceholders(line, rootCommand, 1, 1, 0));
                }
            }
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
        List<String> headerLines = format.getHeaderLines();
        if (headerLines != null) {
            for (String line : headerLines) {
                sender.sendMessage(applyHeaderFooterPlaceholders(line, rootCommand, page, totalPages, visible.size()));
            }
        }

        // Entries
        String entryTemplate = format.getEntryFormat();
        if (entryTemplate != null && !entryTemplate.isEmpty()) {
            for (int i = startIndex; i < endIndex; i++) {
                HelpEntry entry = visible.get(i);
                sender.sendMessage(applyEntryPlaceholders(entryTemplate, entry, rootCommand, page, totalPages));
            }
        }

        // Footer
        List<String> footerLines = (page >= totalPages && format.getLastPageFooterLines() != null && !format.getLastPageFooterLines().isEmpty())
                ? format.getLastPageFooterLines()
                : format.getFooterLines();
        if (footerLines != null) {
            for (String line : footerLines) {
                sender.sendMessage(applyHeaderFooterPlaceholders(line, rootCommand, page, totalPages, visible.size()));
            }
        }
    }

    private String applyHeaderFooterPlaceholders(String template, String rootCommand, int page, int totalPages, int totalEntries) {
        if (template == null) {
            return "";
        }
        int nextPage = Math.min(page + 1, totalPages);
        int prevPage = Math.max(page - 1, 1);

        String result = template
                .replace("%COMMAND%", rootCommand.toUpperCase())
                .replace("{COMMAND}", rootCommand.toUpperCase())
                .replace("%ROOT%", rootCommand.toUpperCase())
                .replace("{ROOT}", rootCommand.toUpperCase())
                .replace("%command%", rootCommand)
                .replace("{command}", rootCommand)
                .replace("%root%", rootCommand)
                .replace("{root}", rootCommand)
                .replace("%page%", String.valueOf(page))
                .replace("{page}", String.valueOf(page))
                .replace("%maxPages%", String.valueOf(totalPages))
                .replace("{maxPages}", String.valueOf(totalPages))
                .replace("%totalPages%", String.valueOf(totalPages))
                .replace("{totalPages}", String.valueOf(totalPages))
                .replace("%nextPage%", String.valueOf(nextPage))
                .replace("{nextPage}", String.valueOf(nextPage))
                .replace("%prevPage%", String.valueOf(prevPage))
                .replace("{prevPage}", String.valueOf(prevPage))
                .replace("%totalEntries%", String.valueOf(totalEntries))
                .replace("{totalEntries}", String.valueOf(totalEntries));

        return ChatColor.translateAlternateColorCodes('&', result);
    }

    private String applyEntryPlaceholders(String template, HelpEntry entry, String rootCommand, int page, int totalPages) {
        if (template == null) {
            return "";
        }

        String result = template;
        String desc = entry.getDescription();
        if (desc == null || desc.trim().isEmpty()) {
            result = result.replaceAll("(?i)(\\s*(&[0-9a-fk-or])?\\s*-\\s*)?%description%", "");
            result = result.replaceAll("(?i)(\\s*(&[0-9a-fk-or])?\\s*-\\s*)?\\{description\\}", "");
        } else {
            result = result.replace("%description%", desc).replace("{description}", desc);
        }

        result = result
                .replace("%COMMAND%", rootCommand.toUpperCase())
                .replace("{COMMAND}", rootCommand.toUpperCase())
                .replace("%root%", rootCommand)
                .replace("{root}", rootCommand)
                .replace("%syntax%", entry.getSyntax())
                .replace("{syntax}", entry.getSyntax())
                .replace("%command%", entry.getSyntax())
                .replace("{command}", entry.getSyntax())
                .replace("%permission%", entry.getPermission() != null ? entry.getPermission() : "")
                .replace("{permission}", entry.getPermission() != null ? entry.getPermission() : "")
                .replace("%page%", String.valueOf(page))
                .replace("{page}", String.valueOf(page))
                .replace("%maxPages%", String.valueOf(totalPages))
                .replace("{maxPages}", String.valueOf(totalPages))
                .replace("%totalPages%", String.valueOf(totalPages))
                .replace("{totalPages}", String.valueOf(totalPages));

        return ChatColor.translateAlternateColorCodes('&', result);
    }

    public HelpFormat getFormat() {
        return format;
    }

    public void setFormat(HelpFormat format) {
        this.format = (format != null) ? format : HelpFormat.defaultFormat();
    }

    public void setFormat(String header, String entry, String footer) {
        setFormat(new HelpFormat(header, entry, footer));
    }

    public void setFormat(String header, String entry, String footer, String emptyMessage) {
        setFormat(new HelpFormat(header, entry, footer, emptyMessage));
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
