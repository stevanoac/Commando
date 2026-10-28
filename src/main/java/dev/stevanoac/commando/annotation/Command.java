package dev.stevanoac.commando.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class or method as a command route.
 * <p>
 * Routes are specified via whitespace-separated paths:
 * <ul>
 *     <li>Class-level {@code @Command(name = "arena")} sets the root context.</li>
 *     <li>Method-level {@code @Command(name = "arena delete")} or {@code @Command("delete")} registers nested paths.</li>
 * </ul>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {

    /**
     * The whitespace-separated route path of the command (e.g. "arena", "arena delete", "delete").
     */
    String name() default "";

    /**
     * Alias for {@link #name()}.
     */
    String value() default "";

    /**
     * Alternative aliases for this route.
     */
    String[] aliases() default {};

    /**
     * Required permission node to execute the command.
     * Empty string indicates no permission is required.
     */
    String permission() default "";

    /**
     * Custom message sent when sender lacks permission.
     * If empty, the framework's default permission message is used.
     */
    String permissionMessage() default "";

    /**
     * Description of the command used in help listings.
     */
    String description() default "";

    /**
     * Usage syntax display. If empty, syntax is generated automatically.
     */
    String usage() default "";

    /**
     * Whether console/terminal senders can execute this command.
     * Defaults to false (player-only).
     */
    boolean console() default false;

    /**
     * Cooldown in seconds before the command can be used again.
     * Defaults to 0 (no cooldown).
     */
    long cooldown() default 0;
}
