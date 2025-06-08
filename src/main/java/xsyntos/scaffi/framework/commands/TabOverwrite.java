package xsyntos.scaffi.framework.commands;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * An annotation to overwrite the default taboverwrite for a param.
 */
@Retention(RetentionPolicy.RUNTIME) // Specifies when the annotation is retained (at runtime)
@Target(ElementType.PARAMETER)
public @interface TabOverwrite {
    /**
     * A class that implements ITabOverwriter
     * @return
     */
    Class<? extends ITabOverwriter> value();
}
