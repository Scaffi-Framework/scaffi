package nl.xsyntos.scaffi.framework.commands.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME) // Specifies when the annotation is retained (at runtime)
@Target(ElementType.PARAMETER)
public @interface CommandNullable {
    
}
