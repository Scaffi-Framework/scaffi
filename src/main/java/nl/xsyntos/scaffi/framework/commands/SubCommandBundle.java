package nl.xsyntos.scaffi.framework.commands;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.bukkit.command.CommandSender;

import lombok.AllArgsConstructor;
import lombok.Data;
import nl.xsyntos.scaffi.framework.commands.annotations.Async;
import nl.xsyntos.scaffi.framework.commands.annotations.SubCommand;
import nl.xsyntos.scaffi.framework.exceptions.InternalCommandException;

@Data
@AllArgsConstructor
public class SubCommandBundle {
    private SubCommand subCommand;
    private Method method;

    public boolean isRoot() {
        return subCommand.command().isBlank();
    }

    public boolean isAsync() {
        return method.isAnnotationPresent(Async.class);
    }

    public CommandResponse getAsyncResponse(CommandContext context) {
        Async responseClass = method.getAnnotation(Async.class);
        try {
            return responseClass.value().getConstructor().newInstance().response(context);
        } catch (Exception e) {
            e.printStackTrace();
            throw new InternalCommandException(e);
        }
    }

    public CommandResponse invoke(Object instance, Object... params) throws Exception {
        return (CommandResponse) method.invoke(instance, params);
    }

    public Parameter[] getParameters() {
        return method.getParameters();
    }

    public boolean executorAllowed(CommandSender sender) {
        for(Class<? extends CommandSender> clazz : subCommand.executorWhitelist()) {
            if(clazz.isInstance(sender))
                return true;
        }
        return false;
    }
}
