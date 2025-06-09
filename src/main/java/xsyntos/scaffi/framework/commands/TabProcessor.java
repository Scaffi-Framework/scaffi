package xsyntos.scaffi.framework.commands;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import lombok.AllArgsConstructor;

@AllArgsConstructor
class TabProcessor implements TabCompleter {
    private HashMap<String, SubCommandBundle> subCommands;
    private xsyntos.scaffi.framework.commands.Command command;

    @Override
    public List<String> onTabComplete(CommandSender arg0, org.bukkit.command.Command arg1, String arg2, String[] arg3) {
        CommandContext context = new CommandContext(arg0, arg3, arg2);
        List<String> list = findAndExecuteTabComplete(context);
        
        list.removeIf(s -> !s.toLowerCase().startsWith(arg3[arg3.length - 1].toLowerCase()));
        return list;
    }

    private List<String> findAndExecuteTabComplete(CommandContext context) {
        if(!command.permission().isEmpty() && !context.getSender().hasPermission(command.permission())) 
            return Collections.emptyList();
        
        ArrayList<String> list = new ArrayList<>();

        SubCommandBundle subCommand = null;
        if(context.getArgs().length > 0 && !context.getArgs()[0].isEmpty()) 
            subCommand = subCommands.get(context.getArgs()[0]);
        
        boolean usingDefault = subCommand == null;
        if(usingDefault) 
            subCommand = subCommands.get("");

        if(subCommand != null)
            list.addAll(executeTabComplete(context, subCommand, usingDefault));

        if(context.getArgs().length == 1) {
            list.addAll(subCommands.keySet());
        }

        list.removeIf(i -> !i.toLowerCase().startsWith(context.getArgs()[context.getArgs().length - 1].toLowerCase()));
        return list;
    }

    private List<String> executeTabComplete(CommandContext context, SubCommandBundle subCommand, boolean usingDefault) {
        int currentArg = usingDefault ? 0 : 1;

        for(int i = 1; i < subCommand.getMethod().getParameters().length; i++) {
            //get the converter for the parameter
            Parameter param = subCommand.getMethod().getParameters()[i];
            IConverter<?> converter = ConverterRegistry.getConverter(param.getType());

            if(converter == null) {
                Bukkit.getLogger().warning(String.format("Method %s has a parameter of type %s which does not have a converter", subCommand.getMethod().getName(), param.getType().getName()));
                return Collections.emptyList();
            }

            currentArg += converter.getSize();
            if(currentArg >= context.getArgs().length) {
                TabOverwrite overwrite = param.getAnnotation(TabOverwrite.class);

                if(overwrite == null) {
                    return converter.tabComplete(context, context.getArgs()[context.getArgs().length - 1], context.getArgs().length - (currentArg - converter.getSize()) - 1);
                }
                try {   
                    return overwrite.value().getConstructor().newInstance().tabComplete(context, context.getArgs()[context.getArgs().length - 1], context.getArgs().length - (currentArg - converter.getSize()) - 1);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    String command = usingDefault ? "DEFAULT" : context.getArgs()[0];
                    Bukkit.getLogger().warning(String.format("Something went wrong for the custom tabcompleter of command %s, subcommand %s, param %s", context.getAlias(), command ,param.getName()));
                }
            }
        }
        return Collections.emptyList();
    }
    
}
