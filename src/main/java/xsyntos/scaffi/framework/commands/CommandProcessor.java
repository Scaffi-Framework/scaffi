package xsyntos.scaffi.framework.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import lombok.AllArgsConstructor;
import xsyntos.scaffi.framework.ScaffiPlugin;

import java.lang.reflect.Parameter;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import javax.annotation.Nullable;

@AllArgsConstructor
class CommandProcessor implements CommandExecutor {
    private HashMap<String, SubCommandBundle> subCommands;
    private xsyntos.scaffi.framework.commands.Command command;
    private Object instance;

    @Override
    public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
        CommandContext context = new CommandContext(arg0, arg3, arg2);
        CommandResponse response = findAndExecuteSubCommand(context);
        response.send(context);
        return true;
    }

    private CommandResponse findAndExecuteSubCommand(CommandContext context) {
        if(!command.permission().isEmpty() && !context.getSender().hasPermission(command.permission())) 
            return ScaffiPlugin.config.getMessages().getNoPermissions();
        
        
        if(context.getArgs().length == 0) {
            SubCommandBundle subCommand = subCommands.get("");
            if(subCommand != null) {
                return executeCommand(context, subCommand);
            } else {
                return ScaffiPlugin.config.getMessages().getInvalidUsage();
            }

        }

        SubCommandBundle subCommand = subCommands.get(context.getArgs()[0]);
        boolean usingDefault = subCommand == null;
        if(usingDefault) 
            subCommand = subCommands.get("");

        if(subCommand == null) {
            Bukkit.getLogger().warning(String.format("No command to run found!"));
            return ScaffiPlugin.config.getMessages().getServerError();
        }

        ArrayList<Object> params = new ArrayList<>();
        int currentArg = usingDefault ? 0 : 1;

        for(int i = 1; i < subCommand.getMethod().getParameters().length; i++) {
            //get the converter for the parameter
            Parameter param = subCommand.getMethod().getParameters()[i];
            IConverter<?> converter = ConverterRegistry.getConverter(param.getType());

            if(converter == null) {
                Bukkit.getLogger().warning(String.format("Method %s has a parameter of type %s which does not have a converter", subCommand.getMethod().getName(), param.getType().getName()));
                return ScaffiPlugin.config.getMessages().getServerError();
            }

            try {
                if(converter.getSize() == 1) {
                    params.add(converter.convert(context.getArgs()[currentArg]));
                } else {
                    String value = "";
                    for(int j = 0; j < converter.getSize(); j++) {
                        value += context.getArgs()[currentArg + j] + " ";
                    }
                    params.add(converter.convert(value.trim()));
                }

                currentArg += converter.getSize();
            } catch (ArrayIndexOutOfBoundsException e) {
                if(param.isAnnotationPresent(Nullable.class)) {
                    params.add(null); 
                } else {
                    return ScaffiPlugin.config.getMessages().getInvalidUsage();
                }
            } catch (Exception e) {
                return converter.onError(context.getArgs()[currentArg]);
            }
        }

        if(currentArg != context.getArgs().length && (!command.allowExtraArgs() || !subCommand.getSubCommand().allowExtraArgs())) {
            return ScaffiPlugin.config.getMessages().getInvalidUsage();
        }

        return executeCommand(context, subCommand, params);
    }

    private CommandResponse executeCommand(CommandContext context, SubCommandBundle bundle, List<Object> params) {
        if(params.size() == 0)
            return executeCommand(context, bundle);

        if(bundle.getSubCommand().permission().isEmpty() || context.getSender().hasPermission(bundle.getSubCommand().permission())) {
            try {
                //add context as first parameter
                params.add(0, context);

                return (CommandResponse) bundle.getMethod().invoke(instance, params.toArray());
            } catch (Exception e) {
                e.printStackTrace();
                return ScaffiPlugin.config.getMessages().getServerError();
            }
        } else {
            return ScaffiPlugin.config.getMessages().getNoPermissions();
        }

    }


    private CommandResponse executeCommand(CommandContext context, SubCommandBundle bundle) {
        if(bundle.getSubCommand().permission().isEmpty() || context.getSender().hasPermission(bundle.getSubCommand().permission())) {
            try {
                return (CommandResponse) bundle.getMethod().invoke(instance, context);
            } catch (Exception e) {
                e.printStackTrace();
                return ScaffiPlugin.config.getMessages().getServerError();
            }
        } else {
            return ScaffiPlugin.config.getMessages().getNoPermissions();
        }
    }

}
