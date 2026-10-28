package dev.stevanoac.commando.adapter;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Adapter interface responsible for parsing raw string arguments into typed objects
 * and providing contextual tab-completions.
 *
 * @param <T> the parsed argument type
 */
public interface ArgumentAdapter<T> {

    /**
     * Parses the raw string input into the target type.
     *
     * @param sender the command sender executing the command
     * @param input the raw string token from arguments
     * @return the parsed object of type {@code T}
     * @throws IllegalArgumentException if the input cannot be parsed or is invalid
     */
    T parse(CommandSender sender, String input) throws IllegalArgumentException;

    /**
     * Suggests tab-completion options for the current input token.
     *
     * @param sender the command sender tab-completing the argument
     * @param currentInput the partially typed input token
     * @return list of completion suggestions
     */
    List<String> suggest(CommandSender sender, String currentInput);
}
