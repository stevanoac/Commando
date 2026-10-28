package dev.stevanoac.commando.adapter;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Built-in adapter for integer arguments.
 */
public class IntegerAdapter implements ArgumentAdapter<Integer> {

    @Override
    public Integer parse(CommandSender sender, String input) throws IllegalArgumentException {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + input + "' is not a valid whole number.");
        }
    }

    @Override
    public List<String> suggest(CommandSender sender, String currentInput) {
        return Collections.emptyList();
    }
}
