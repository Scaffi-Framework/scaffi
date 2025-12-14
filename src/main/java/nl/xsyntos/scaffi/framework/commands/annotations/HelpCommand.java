package nl.xsyntos.scaffi.framework.commands.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HelpCommand {
    int entriesPerPage() default 5;
    String headerText() default "Help Page";
    char primaryColor() default '6';
    char secondaryColor() default 'e';
}
