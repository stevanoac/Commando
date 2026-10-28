package dev.stevanoac.commando.adapter;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in adapter for resolving online Bukkit Player entities.
 */
public class PlayerAdapter implements ArgumentAdapter<Player> {

    @Override
    public Player parse(CommandSender sender, String input) throws IllegalArgumentException {
        Player player = Bukkit.getPlayerExact(input);
        if (player == null) {
            player = Bukkit.getPlayer(input);
        }
        if (player == null) {
            throw new IllegalArgumentException("Player '" + input + "' is not online.");
        }
        return player;
    }

    @Override
    public List<String> suggest(CommandSender sender, String currentInput) {
        String lower = currentInput.toLowerCase();
        List<String> matched = new ArrayList<>();

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (sender instanceof Player && !((Player) sender).canSee(online)) {
                continue;
            }
            if (online.getName().toLowerCase().startsWith(lower)) {
                matched.add(online.getName());
            }
        }

        return matched;
    }
}
