package xsyntos.scaffi.framework.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to register a Java Class as a Minecraft SubCommand
 */
@Retention(RetentionPolicy.RUNTIME) // Specifies when the annotation is retained (at runtime)
@Target(ElementType.METHOD)
public @interface SubCommand {
    /**
     * The name of the command
     */
    String command() default "";
    /**
     * The permission of the command. If not set, the command evoker doesn't need permissions.
     */
    String permission() default "";
    /**
     * Description of the command
     */
    String description() default "";
    /**
     * Aliases of the command. These can be used instead of the command-name
     */
    String[] aliases() default {};
    /**
     * Determine whether to permit command argument overflow
     */
    boolean allowExtraArgs() default true;
}
