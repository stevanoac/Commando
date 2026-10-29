package dev.stevanoac.commando.internal;

import dev.stevanoac.commando.Commando;
import dev.stevanoac.commando.adapter.ArgumentAdapter;
import dev.stevanoac.commando.argument.CommandArgs;
import dev.stevanoac.commando.cooldown.CooldownManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.logging.Level;

/**
 * Executes a target command method, managing parameter injection, type adapters,
 * permissions, console rules, and cooldowns.
 */
public class RouteHandler {

    private final Method method;
    private final Object instance;
    private final List<ParameterResolver> allResolvers = new ArrayList<>();
    private final List<ParameterResolver> adapterResolvers = new ArrayList<>();

    private final String fullRoute;
    private final String permission;
    private final String permissionMessage;
    private final boolean console;
    private final long cooldown;
    private final String description;
    private final String usage;

    public RouteHandler(Method method, Object instance, String fullRoute,
                        String permission, String permissionMessage,
                        boolean console, long cooldown, String description, String usage) {
        this.method = method;
        this.instance = instance;
        this.fullRoute = fullRoute;
        this.permission = permission != null ? permission : "";
        this.permissionMessage = permissionMessage != null ? permissionMessage : "";
        this.console = console;
        this.cooldown = cooldown;
        this.description = description != null ? description : "";
        this.usage = usage != null ? usage : "";

        Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length; i++) {
            ParameterResolver resolver = new ParameterResolver(params[i], i);
            allResolvers.add(resolver);
            if (resolver.isAdapter() || resolver.isVarargsString()) {
                adapterResolvers.add(resolver);
            }
        }
    }

    public boolean execute(CommandSender sender, String label, String[] remainingArgs, Commando framework) {
        // 1. Console vs Player check
        if (!console && !(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', framework.getPlayerOnlyMessage()));
            return true;
        }

        // 2. Permission check
        if (!permission.isEmpty() && !sender.hasPermission(permission)) {
            String permMsg = !permissionMessage.isEmpty() ? permissionMessage : framework.getNoPermissionMessage();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', permMsg));
            return true;
        }

        // 3. Cooldown check
        if (cooldown > 0 && sender instanceof Player) {
            Player player = (Player) sender;
            CooldownManager cdManager = framework.getCooldownManager();
            String cdKey = fullRoute.replace(' ', ':').toLowerCase();

            if (cdManager.isOnCooldown(player.getUniqueId(), cdKey)) {
                long remainingSec = cdManager.getRemainingSeconds(player.getUniqueId(), cdKey);
                double remainingPrecise = cdManager.getRemainingSecondsPrecise(player.getUniqueId(), cdKey);
                String msg = framework.getCooldownMessage()
                        .replace("{seconds}", String.valueOf(remainingSec))
                        .replace("{time}", String.format(Locale.ROOT, "%.1f", remainingPrecise))
                        .replace("{command}", label);
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                return true;
            }

            cdManager.applyCooldown(player.getUniqueId(), cdKey, cooldown);
        }

        // 4. Parameter Resolution
        Object[] argsToPass = new Object[allResolvers.size()];
        int argIndex = 0;

        for (int i = 0; i < allResolvers.size(); i++) {
            ParameterResolver resolver = allResolvers.get(i);

            if (resolver.isSender()) {
                argsToPass[i] = sender;
                continue;
            }

            if (resolver.isPlayer()) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', framework.getPlayerOnlyMessage()));
                    return true;
                }
                argsToPass[i] = sender;
                continue;
            }

            if (resolver.isCommandArgs()) {
                Player player = (sender instanceof Player) ? (Player) sender : null;
                argsToPass[i] = new CommandArgs(sender, player, label, remainingArgs);
                continue;
            }

            if (resolver.isVarargsString()) {
                String[] rest = (argIndex < remainingArgs.length)
                        ? Arrays.copyOfRange(remainingArgs, argIndex, remainingArgs.length)
                        : new String[0];
                argsToPass[i] = rest;
                argIndex = remainingArgs.length;
                continue;
            }

            // Adapter-backed parameter
            if (argIndex >= remainingArgs.length) {
                String syntax = getUsageSyntax(label);
                sender.sendMessage(ChatColor.RED + "Missing required argument: <" + resolver.getName() + ">");
                sender.sendMessage(ChatColor.RED + "Usage: " + syntax);
                return true;
            }

            String raw = remainingArgs[argIndex];
            ArgumentAdapter<?> adapter = framework.getAdapter(resolver.getTargetClass());
            if (adapter == null) {
                throw new IllegalStateException("No ArgumentAdapter registered for type: "
                        + resolver.getTargetClass().getName());
            }

            try {
                Object parsed = adapter.parse(sender, raw);
                argsToPass[i] = parsed;
                argIndex++;
            } catch (IllegalArgumentException e) {
                String syntax = getUsageSyntax(label);
                String errorMsg = (e.getMessage() != null) ? e.getMessage() : "Invalid parameter: " + raw;
                sender.sendMessage(ChatColor.RED + errorMsg);
                sender.sendMessage(ChatColor.RED + "Usage: " + syntax);
                return true;
            }
        }

        // 5. Invoke method
        try {
            method.setAccessible(true);
            method.invoke(instance, argsToPass);
            return true;
        } catch (InvocationTargetException e) {
            Throwable cause = (e.getCause() != null) ? e.getCause() : e;
            framework.getPlugin().getLogger().log(Level.SEVERE, "Exception executing command route: " + fullRoute, cause);
            sender.sendMessage(ChatColor.RED + "An internal error occurred while executing this command.");
            return true;
        } catch (Exception e) {
            framework.getPlugin().getLogger().log(Level.SEVERE, "Failed to invoke command method: " + method.getName(), e);
            sender.sendMessage(ChatColor.RED + "An internal error occurred while executing this command.");
            return true;
        }
    }

    public String getUsageSyntax(String label) {
        if (!usage.isEmpty()) {
            return ChatColor.translateAlternateColorCodes('&', usage.replace("{label}", label));
        }

        StringBuilder sb = new StringBuilder("/").append(fullRoute);
        for (ParameterResolver resolver : adapterResolvers) {
            sb.append(" <").append(resolver.getName()).append(">");
        }
        return sb.toString();
    }

    public List<ParameterResolver> getAdapterResolvers() {
        return adapterResolvers;
    }

    public String getFullRoute() {
        return fullRoute;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isConsole() {
        return console;
    }

    public String getDescription() {
        return description;
    }

    public Method getMethod() {
        return method;
    }

    public Object getInstance() {
        return instance;
    }
}
