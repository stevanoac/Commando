package dev.stevanoac.commando.internal;

import dev.stevanoac.commando.annotation.Help;
import dev.stevanoac.commando.help.HelpPaginator;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Invokes custom methods annotated with {@link Help}.
 */
public class HelpHandler {

    private final Method method;
    private final Object instance;
    private final int entriesPerPage;

    public HelpHandler(Method method, Object instance, Help annotation) {
        this.method = method;
        this.instance = instance;
        this.entriesPerPage = annotation.entriesPerPage();
    }

    public void invoke(CommandSender sender, HelpPaginator paginator, int page, Logger logger) {
        try {
            method.setAccessible(true);
            Class<?>[] paramTypes = method.getParameterTypes();
            Object[] args = new Object[paramTypes.length];

            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> type = paramTypes[i];
                if (type.equals(CommandSender.class)) {
                    args[i] = sender;
                } else if (type.equals(Player.class)) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "This command can only be executed by players.");
                        return;
                    }
                    args[i] = sender;
                } else if (type.equals(HelpPaginator.class)) {
                    args[i] = paginator;
                } else if (type.equals(int.class) || type.equals(Integer.class)) {
                    args[i] = page;
                }
            }

            method.invoke(instance, args);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to invoke custom @Help handler " + method.getName(), e);
            paginator.sendHelp(sender, page);
        }
    }

    public int getEntriesPerPage() {
        return entriesPerPage;
    }

    public Method getMethod() {
        return method;
    }

    public Object getInstance() {
        return instance;
    }
}
