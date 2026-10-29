package dev.stevanoac.commando.internal;

import dev.stevanoac.commando.Commando;
import dev.stevanoac.commando.adapter.ArgumentAdapter;
import dev.stevanoac.commando.annotation.Command;
import dev.stevanoac.commando.help.HelpEntry;
import dev.stevanoac.commando.help.HelpPaginator;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Root route tree managing path-based subcommands, automatic tab-completion,
 * and paginated help routing.
 */
public class CommandTree {

    private final String rootName;
    private final List<String> aliases = new ArrayList<>();
    private final String description;
    private final String usage;
    private final String permission;
    private final String permissionMessage;
    private final boolean console;
    private final long cooldown;
    private final Commando framework;

    private final RouteNode rootNode;
    private HelpHandler helpHandler;
    private HelpPaginator cachedPaginator;

    public CommandTree(Commando framework, Command annotation, String rootName) {
        this.framework = Objects.requireNonNull(framework, "CommandFramework cannot be null");
        this.rootName = rootName.toLowerCase();
        this.rootNode = new RouteNode(this.rootName, null);

        this.description = annotation.description();
        this.usage = annotation.usage();
        this.permission = annotation.permission();
        this.permissionMessage = annotation.permissionMessage();
        this.console = annotation.console();
        this.cooldown = annotation.cooldown();

        for (String alias : annotation.aliases()) {
            this.aliases.add(alias.toLowerCase());
        }
    }

    public void registerRoute(String fullPath, Command annotation, Method method, Object instance) {
        String[] tokens = fullPath.trim().split("\\s+");
        if (tokens.length == 0) {
            return;
        }

        RouteNode current = rootNode;
        for (int i = 1; i < tokens.length; i++) {
            current = current.getOrCreateChild(tokens[i]);
        }

        for (String alias : annotation.aliases()) {
            if (current.getParent() != null) {
                current.getParent().addAlias(alias, current);
            } else {
                aliases.add(alias.toLowerCase());
            }
        }

        String routePermission = !annotation.permission().isEmpty() ? annotation.permission() : this.permission;
        String routePermMsg = !annotation.permissionMessage().isEmpty() ? annotation.permissionMessage() : this.permissionMessage;
        boolean routeConsole = annotation.console() || this.console;
        long routeCooldown = annotation.cooldown() > 0 ? annotation.cooldown() : this.cooldown;

        RouteHandler handler = new RouteHandler(
                method,
                instance,
                fullPath,
                routePermission,
                routePermMsg,
                routeConsole,
                routeCooldown,
                annotation.description(),
                annotation.usage()
        );

        current.setHandler(handler);
        cachedPaginator = null; // Invalidate cached paginator
    }

    public void setHelpHandler(HelpHandler helpHandler) {
        this.helpHandler = helpHandler;
        this.cachedPaginator = null;
    }

    public HelpPaginator getPaginator() {
        if (cachedPaginator == null) {
            List<HelpEntry> entries = new ArrayList<>();
            rootNode.collectHelpEntries(entries);
            int perPage = (helpHandler != null) ? helpHandler.getEntriesPerPage() : 6;
            cachedPaginator = new HelpPaginator(rootName, perPage, entries);
        }
        return cachedPaginator;
    }

    public boolean execute(CommandSender sender, String label, String[] args) {
        // Fallback or explicit help route: /<root> help [page]
        if (args.length >= 1 && args[0].equalsIgnoreCase("help")) {
            RouteNode explicitHelp = rootNode.findChild("help");
            if (explicitHelp == null || !explicitHelp.hasHandler()) {
                int page = 1;
                if (args.length >= 2) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {}
                }

                if (helpHandler != null) {
                    helpHandler.invoke(sender, getPaginator(), page, framework.getPlugin().getLogger());
                } else {
                    getPaginator().sendHelp(sender, page);
                }
                return true;
            }
        }

        // Traverse path-tree
        RouteNode current = rootNode;
        int matchedTokens = 0;

        for (int i = 0; i < args.length; i++) {
            RouteNode child = current.findChild(args[i]);
            if (child != null) {
                current = child;
                matchedTokens++;
            } else {
                break;
            }
        }

        if (current.hasHandler()) {
            String[] remainingArgs = (matchedTokens < args.length)
                    ? Arrays.copyOfRange(args, matchedTokens, args.length)
                    : new String[0];
            return current.getHandler().execute(sender, label, remainingArgs, framework);
        }

        // If args are empty and rootNode has a handler
        if (args.length == 0 && rootNode.hasHandler()) {
            return rootNode.getHandler().execute(sender, label, args, framework);
        }

        // Console check before showing help
        if (!console && !(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', framework.getPlayerOnlyMessage()));
            return true;
        }

        // Permission check before showing help
        if (!permission.isEmpty() && !sender.hasPermission(permission)) {
            String permMsg = !permissionMessage.isEmpty() ? permissionMessage : framework.getNoPermissionMessage();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', permMsg));
            return true;
        }

        // Dispatch help paginator as default fallback
        getPaginator().sendHelp(sender, 1);
        return true;
    }

    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }

        String[] pathPrefix = (args.length > 1) ? Arrays.copyOfRange(args, 0, args.length - 1) : new String[0];
        String tokenToComplete = args[args.length - 1];

        // Walk matched path nodes
        RouteNode current = rootNode;
        int matched = 0;

        for (int i = 0; i < pathPrefix.length; i++) {
            RouteNode child = current.findChild(pathPrefix[i]);
            if (child != null) {
                current = child;
                matched++;
            } else {
                break;
            }
        }

        List<String> suggestions = new ArrayList<>();

        if (matched == pathPrefix.length) {
            // Suggest child subcommands
            for (RouteNode child : current.getUniqueChildren()) {
                if (child.hasHandler()) {
                    RouteHandler h = child.getHandler();
                    if (!h.isConsole() && !(sender instanceof Player)) {
                        continue;
                    }
                    if (!h.getPermission().isEmpty() && !sender.hasPermission(h.getPermission())) {
                        continue;
                    }
                }
                suggestions.add(child.getToken());
                suggestions.addAll(child.getAliases());
            }

            if (current == rootNode) {
                suggestions.add("help");
            }

            // If current node has a handler, suggest for its first adapter parameter
            if (current.hasHandler()) {
                List<ParameterResolver> adapters = current.getHandler().getAdapterResolvers();
                if (!adapters.isEmpty()) {
                    ArgumentAdapter<?> adapter = framework.getAdapter(adapters.get(0).getTargetClass());
                    if (adapter != null) {
                        suggestions.addAll(adapter.suggest(sender, tokenToComplete));
                    }
                }
            }
        } else {
            // Path prefix already diverged into parameters of current node
            if (current.hasHandler()) {
                int argPos = pathPrefix.length - matched;
                List<ParameterResolver> adapters = current.getHandler().getAdapterResolvers();
                if (argPos >= 0 && argPos < adapters.size()) {
                    ArgumentAdapter<?> adapter = framework.getAdapter(adapters.get(argPos).getTargetClass());
                    if (adapter != null) {
                        suggestions.addAll(adapter.suggest(sender, tokenToComplete));
                    }
                }
            }
        }

        // Special help page tab-completion: /<root> help [page]
        if (pathPrefix.length == 1 && pathPrefix[0].equalsIgnoreCase("help")) {
            int totalPages = getPaginator().getTotalPages(sender);
            for (int p = 1; p <= totalPages; p++) {
                suggestions.add(String.valueOf(p));
            }
        }

        List<String> matchedList = new ArrayList<>();
        StringUtil.copyPartialMatches(tokenToComplete, suggestions, matchedList);
        Collections.sort(matchedList);
        return matchedList;
    }

    public String getRootName() {
        return rootName;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public RouteNode getRootNode() {
        return rootNode;
    }
}
