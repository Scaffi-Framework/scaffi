package xsyntos.scaffi.framework.commands;

import java.lang.reflect.Method;

import lombok.AllArgsConstructor;
import lombok.Data;
import xsyntos.scaffi.framework.commands.annotations.Async;
import xsyntos.scaffi.framework.commands.annotations.SubCommand;
import xsyntos.scaffi.framework.exceptions.InternalCommandException;

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
}
