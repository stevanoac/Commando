package dev.stevanoac.commando.help;

import java.util.Objects;

/**
 * Container holding metadata for a registered command entry displayed in help menus.
 */
public class HelpEntry {

    private final String syntax;
    private final String description;
    private final String permission;
    private final boolean consoleAllowed;

    public HelpEntry(String syntax, String description, String permission, boolean consoleAllowed) {
        this.syntax = Objects.requireNonNull(syntax, "Syntax cannot be null");
        this.description = (description != null) ? description : "";
        this.permission = (permission != null) ? permission : "";
        this.consoleAllowed = consoleAllowed;
    }

    public String getSyntax() {
        return syntax;
    }

    public String getDescription() {
        return description;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isConsoleAllowed() {
        return consoleAllowed;
    }
}
