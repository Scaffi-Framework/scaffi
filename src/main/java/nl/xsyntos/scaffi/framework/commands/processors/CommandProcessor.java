package nl.xsyntos.scaffi.framework.commands.processors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import lombok.AllArgsConstructor;
import nl.xsyntos.scaffi.framework.ScaffiPlugin;
import nl.xsyntos.scaffi.framework.commands.CommandContext;
import nl.xsyntos.scaffi.framework.commands.CommandResponse;
import nl.xsyntos.scaffi.framework.commands.SubCommandBundle;
import nl.xsyntos.scaffi.framework.commands.converters.IConverter;
import nl.xsyntos.scaffi.framework.exceptions.CommandUsageException;
import nl.xsyntos.scaffi.framework.exceptions.InternalCommandException;
import nl.xsyntos.scaffi.framework.exceptions.UnableConvertException;

import java.lang.reflect.Parameter;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
public class CommandProcessor implements CommandExecutor {
    private HashMap<String, SubCommandBundle> subCommands;
    private nl.xsyntos.scaffi.framework.commands.annotations.Command command;
    private Object instance;

    @Override
    public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
        CommandContext context = new CommandContext(arg0, arg3, arg2);
        CommandResponse response = findAndExecuteSubCommand(context);
        response.send(context);
        return true;
    }

    private CommandResponse findAndExecuteSubCommand(CommandContext context) {
        //Validate Permissions
        if(!command.permission().isEmpty() && !context.getSender().hasPermission(command.permission())) 
            return ScaffiPlugin.config.getMessages().getNoPermissions();

        SubCommandBundle bundle = this.findSubCommand(context);

        if(bundle == null)
            return ScaffiPlugin.config.getMessages().getInvalidUsage();

        if(!bundle.executorAllowed(context.getSender()))
            return ScaffiPlugin.config.getMessages().getExecutorNotAllowed();

        if(bundle.isAsync()) {
            CompletableFuture<CommandResponse> future = new CompletableFuture<>();
            Bukkit.getScheduler().runTaskAsynchronously(ScaffiPlugin.config.getPlugin(), () -> {
                future.complete(this.executeSubCommand(context, bundle));
            });
            future.thenAccept((response) -> {
                Bukkit.getScheduler().runTask(ScaffiPlugin.config.getPlugin(), () -> response.send(context));
            });
            return bundle.getAsyncResponse(context);

        } else {
            return this.executeSubCommand(context, bundle);
        }

    }

    private CommandResponse executeSubCommand(CommandContext context, SubCommandBundle bundle) {
        try {
            Object[] params = this.convertParamsForSubCommand(context, bundle);
            return this.invokeCommand(context, bundle, params);
        } catch(CommandUsageException ex) {
            return ex.getCommandResponse();
        } catch(InternalCommandException ex) {
            ex.getException().printStackTrace();
            return ScaffiPlugin.config.getMessages().getServerError();
        } catch (Exception ex) {
            ex.printStackTrace();
            return ScaffiPlugin.config.getMessages().getServerError();
        }
    }

    @Nullable
    private SubCommandBundle findSubCommand(CommandContext context) {
        if(context.getArgs().length == 0) 
            return subCommands.get("");

        SubCommandBundle subCommand = subCommands.get(context.getArgs()[0]);
        
        if(subCommand == null)
            return subCommands.get("");
        return subCommand;
    }

    private Object[] convertParamsForSubCommand(CommandContext context, SubCommandBundle subCommand) throws UnableConvertException, CommandUsageException {
        Object[] params = new Object[subCommand.getParameterCount()];
        params[0] = context;
        int currentArg = subCommand.isRoot() ? 0 : 1;

        for(int i = 1; i < subCommand.getParameterCount(); i++) {
            //get the converter for the parameter
            var info = subCommand.getParameterInfo(i);
            Parameter param = info.parameter();
            IConverter<?> converter = subCommand.getConverter(i);

            if(converter == null) {
                throw new InternalCommandException(
                    new NullPointerException(
                        String.format("Method %s has a parameter of type %s which does not have a converter", 
                            subCommand.getMethod().getName(), param.getType().getName()
                )));
            }

            try {
                if(converter.getSize() == 1) {
                    params[i] = converter.convert(context, context.getArgs()[currentArg]);
                } else {
                    StringBuilder value = new StringBuilder();
                    for(int j = 0; j < converter.getSize(); j++) {
                        if (j > 0) value.append(' ');
                        value.append(context.getArgs()[currentArg + j]);
                    }
                    params[i] = converter.convert(context, value.toString().trim());
                }

                currentArg += converter.getSize();
            } catch (ArrayIndexOutOfBoundsException e) {
                if(info.optional()) {
                    params[i] = null;
                } else {
                    throw new CommandUsageException("Missing param", ScaffiPlugin.config.getMessages().getInvalidUsage());
                }
            } catch(Exception ex) {
                throw new CommandUsageException("Unable to convert a param!", converter.onError(context.getArgs()[currentArg], ex));
            }
        }

        //Validates if there is an overflow
        if(currentArg != context.getArgs().length && (!command.allowExtraArgs() || !subCommand.getSubCommand().allowExtraArgs())) {
            throw new CommandUsageException("param overflow!", ScaffiPlugin.config.getMessages().getInvalidUsage());
        }

        return params;
    }

    private CommandResponse invokeCommand(CommandContext context, SubCommandBundle bundle, Object[] params) {
        if(bundle.getSubCommand().permission().isEmpty() || context.getSender().hasPermission(bundle.getSubCommand().permission())) {
            try {
                return bundle.invoke(instance, params);
            } catch (Exception e) {
                e.printStackTrace();
                return bundle.getErrorResponse(context);
            }
        } else {
            return ScaffiPlugin.config.getMessages().getNoPermissions();
        }
    }
}