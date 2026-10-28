package dev.stevanoac.commando.adapter;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

/**
 * Built-in adapter for raw String arguments.
 */
public class StringAdapter implements ArgumentAdapter<String> {

    @Override
    public String parse(CommandSender sender, String input) throws IllegalArgumentException {
        return input;
    }

    @Override
    public List<String> suggest(CommandSender sender, String currentInput) {
        return Collections.emptyList();
    }
}
