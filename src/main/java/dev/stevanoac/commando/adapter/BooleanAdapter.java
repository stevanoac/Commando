package dev.stevanoac.commando.adapter;

import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Built-in adapter for boolean flags and arguments.
 */
public class BooleanAdapter implements ArgumentAdapter<Boolean> {

    private static final List<String> SUGGESTIONS = Arrays.asList("true", "false");

    @Override
    public Boolean parse(CommandSender sender, String input) throws IllegalArgumentException {
        String lower = input.toLowerCase();
        if (lower.equals("true") || lower.equals("yes") || lower.equals("on") || lower.equals("1")) {
            return true;
        }
        if (lower.equals("false") || lower.equals("no") || lower.equals("off") || lower.equals("0")) {
            return false;
        }
        throw new IllegalArgumentException("'" + input + "' must be true or false.");
    }

    @Override
    public List<String> suggest(CommandSender sender, String currentInput) {
        String lower = currentInput.toLowerCase();
        List<String> matches = new ArrayList<>();
        for (String option : SUGGESTIONS) {
            if (option.startsWith(lower)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
