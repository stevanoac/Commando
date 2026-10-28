package dev.stevanoac.commando.internal;

import dev.stevanoac.commando.argument.CommandArgs;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Parameter;

/**
 * Metadata describing how a method parameter should be resolved and injected.
 */
public class ParameterResolver {

    public enum Type {
        SENDER,
        PLAYER,
        COMMAND_ARGS,
        VARARGS_STRING,
        ADAPTER
    }

    private final Type type;
    private final Class<?> targetClass;
    private final String name;

    public ParameterResolver(Parameter parameter, int paramIndex) {
        Class<?> clazz = parameter.getType();
        this.targetClass = clazz;

        if (paramIndex == 0 && clazz.equals(CommandSender.class)) {
            this.type = Type.SENDER;
            this.name = "sender";
        } else if (paramIndex == 0 && clazz.equals(Player.class)) {
            this.type = Type.PLAYER;
            this.name = "player";
        } else if (paramIndex == 0 && clazz.equals(CommandArgs.class)) {
            this.type = Type.COMMAND_ARGS;
            this.name = "args";
        } else if (clazz.equals(String[].class)) {
            this.type = Type.VARARGS_STRING;
            this.name = parameter.isNamePresent() ? parameter.getName() : "args...";
        } else {
            this.type = Type.ADAPTER;
            String paramName = parameter.isNamePresent() ? parameter.getName() : null;
            if (paramName == null || paramName.startsWith("arg")) {
                this.name = clazz.getSimpleName().toLowerCase();
            } else {
                this.name = paramName;
            }
        }
    }

    public Type getType() {
        return type;
    }

    public Class<?> getTargetClass() {
        return targetClass;
    }

    public String getName() {
        return name;
    }

    public boolean isSender() {
        return type == Type.SENDER;
    }

    public boolean isPlayer() {
        return type == Type.PLAYER;
    }

    public boolean isCommandArgs() {
        return type == Type.COMMAND_ARGS;
    }

    public boolean isVarargsString() {
        return type == Type.VARARGS_STRING;
    }

    public boolean isAdapter() {
        return type == Type.ADAPTER;
    }
}
