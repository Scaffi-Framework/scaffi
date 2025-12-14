package nl.xsyntos.scaffi.framework.commands.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotations to determine whether to run a subcommand async
 */
@Retention(RetentionPolicy.RUNTIME) 
@Target(ElementType.METHOD)
public @interface Async {
    Class<? extends ICommandAsync> value() default EmptyCommandAsync.class;
}
