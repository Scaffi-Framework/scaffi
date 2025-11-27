package xsyntos.scaffi.framework.commands.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation to register a Java Class as a Minecraft Command
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Command {
    /**
     * The name of the command
     */
    String command() default "";

    /**
     * Aliases of the command. These can be used instead of the command-name
     */
    String[] aliases() default "";

    /**
     * The permission of the command. If not set, the command evoker doesn't need permissions.
     */
    String permission() default "";

    /**
     * Description of the command
     */
    String description() default "";

    /**
     * Determine whether to permit command argument overflow
     */
    boolean allowExtraArgs() default true;
}
