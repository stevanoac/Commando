package dev.stevanoac.commando.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as the custom help handler for a root command path.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Help {

    /**
     * The root command path this help handler applies to (e.g. "arena").
     */
    String command();

    /**
     * Number of help entries shown per page. Defaults to 6.
     */
    int entriesPerPage() default 6;
}
