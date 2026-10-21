package nl.xsyntos.scaffi.framework.commands;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.bukkit.command.CommandSender;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nl.xsyntos.scaffi.framework.commands.annotations.Async;
import nl.xsyntos.scaffi.framework.commands.annotations.CommandNullable;
import nl.xsyntos.scaffi.framework.commands.annotations.SubCommand;
import nl.xsyntos.scaffi.framework.commands.annotations.TabOverwrite;
import nl.xsyntos.scaffi.framework.commands.converters.IConverter;
import nl.xsyntos.scaffi.framework.exceptions.InternalCommandException;

@Data
@EqualsAndHashCode(of = {"subCommand", "method"})
@ToString(of = {"subCommand", "method"})
public class SubCommandBundle {
    private SubCommand subCommand;
    private Method method;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private ParameterInfo[] parameters;

    /** Metadata resolved once when the command method is assigned. */
    public record ParameterInfo(Parameter parameter, IConverter<?> converter,
                                boolean optional, TabOverwrite tabOverwrite) {}

    public SubCommandBundle(SubCommand subCommand, Method method) {
        this.subCommand = subCommand;
        updateMethod(method);
    }

    public void setMethod(Method method) {
        updateMethod(method);
    }

    private void updateMethod(Method method) {
        this.method = method;
        Parameter[] methodParameters = method == null ? new Parameter[0] : method.getParameters();
        this.parameters = new ParameterInfo[methodParameters.length];
        for (int i = 0; i < methodParameters.length; i++) {
            Parameter parameter = methodParameters[i];
            parameters[i] = new ParameterInfo(parameter, ConverterRegistry.getConverter(parameter.getType()),
                parameter.isAnnotationPresent(CommandNullable.class), parameter.getAnnotation(TabOverwrite.class));
        }
    }

    public int getParameterCount() {
        return parameters.length;
    }

    public ParameterInfo getParameterInfo(int index) {
        return parameters[index];
    }

    public IConverter<?> getConverter(int index) {
        ParameterInfo info = parameters[index];
        return ConverterRegistry.getConverter(info.parameter().getType());
    }

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
        Parameter[] copy = new Parameter[parameters.length];
        for (int i = 0; i < parameters.length; i++)
            copy[i] = parameters[i].parameter();
        return copy;
    }

    public boolean executorAllowed(CommandSender sender) {
        for(Class<? extends CommandSender> clazz : subCommand.executorWhitelist()) {
            if(clazz.isInstance(sender))
                return true;
        }
        return false;
    }

    public CommandResponse getErrorResponse(CommandContext context) {
        try {
            return subCommand.errorResponse().getConstructor().newInstance().response(context);
        } catch (Exception e) {
            e.printStackTrace();
            throw new InternalCommandException(e);
        }
    }
}
