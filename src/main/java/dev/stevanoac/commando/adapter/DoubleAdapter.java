package dev.stevanoac.commando.adapter;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Built-in adapter for decimal number arguments.
 */
public class DoubleAdapter implements ArgumentAdapter<Double> {

    @Override
    public Double parse(CommandSender sender, String input) throws IllegalArgumentException {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + input + "' is not a valid decimal number.");
        }
    }

    @Override
    public List<String> suggest(CommandSender sender, String currentInput) {
        return Collections.emptyList();
    }
}
