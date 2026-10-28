package dev.stevanoac.commando.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as the fallback/default command handler
 * executed when arguments are empty or no subcommand matches.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Default {

    /**
     * Optional permission override for the default action.
     * If empty, inherits class-level permission.
     */
    String permission() default "";

    /**
     * Custom message sent when sender lacks permission.
     * If empty, the framework's default permission message is used.
     */
    String permissionMessage() default "";

    /**
     * Whether console/terminal senders can execute the default command.
     * Defaults to false (player-only).
     */
    boolean console() default false;

    /**
     * Cooldown in seconds before the default handler can be used again.
     * Defaults to 0 (inherits class cooldown if > 0).
     */
    long cooldown() default 0;
}
