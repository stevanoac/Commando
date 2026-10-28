package dev.stevanoac.commando.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as providing tab-completions for a command or subcommand.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Completer {

    /**
     * Name of the subcommand this completer provides suggestions for.
     * Empty string ("") targets the root command.
     */
    String name() default "";

    /**
     * 0-based argument index to target.
     * -1 applies to all argument indices for the given route.
     */
    int index() default -1;
}
