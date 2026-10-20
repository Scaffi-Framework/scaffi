package nl.xsyntos.scaffi.framework.commands.processors;

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import lombok.AllArgsConstructor;
import nl.xsyntos.scaffi.framework.commands.CommandContext;
import nl.xsyntos.scaffi.framework.commands.SubCommandBundle;
import nl.xsyntos.scaffi.framework.commands.annotations.TabOverwrite;
import nl.xsyntos.scaffi.framework.commands.converters.IConverter;

@AllArgsConstructor
public class TabProcessor implements TabCompleter {
    private HashMap<String, SubCommandBundle> subCommands;
    private nl.xsyntos.scaffi.framework.commands.annotations.Command command;

    @Override
    public List<String> onTabComplete(CommandSender arg0, org.bukkit.command.Command arg1, String arg2, String[] arg3) {
        if (arg3.length == 0)
            return Collections.emptyList();
        return findAndExecuteTabComplete(new CommandContext(arg0, arg3, arg2));
    }

    private List<String> findAndExecuteTabComplete(CommandContext context) {
        if(!command.permission().isEmpty() && !context.getSender().hasPermission(command.permission())) 
            return Collections.emptyList();
        
        ArrayList<String> list = new ArrayList<>();
        String prefix = context.getArgs()[context.getArgs().length - 1];

        SubCommandBundle subCommand = null;
        if(context.getArgs().length > 0 && !context.getArgs()[0].isEmpty()) 
            subCommand = subCommands.get(context.getArgs()[0]);
        
        boolean usingDefault = subCommand == null;
        if(usingDefault) 
            subCommand = subCommands.get("");

        if(subCommand != null)
            addMatching(list, executeTabComplete(context, subCommand, usingDefault), prefix);

        if(context.getArgs().length == 1) {
            addMatching(list, subCommands.keySet(), prefix);
        }

        return list;
    }

    private void addMatching(List<String> result, Iterable<String> candidates, String prefix) {
        for (String candidate : candidates) {
            if (candidate.regionMatches(true, 0, prefix, 0, prefix.length()))
                result.add(candidate);
        }
    }

    private List<String> executeTabComplete(CommandContext context, SubCommandBundle subCommand, boolean usingDefault) {
        int currentArg = usingDefault ? 0 : 1;

        for(int i = 1; i < subCommand.getParameterCount(); i++) {
            //get the converter for the parameter
            var info = subCommand.getParameterInfo(i);
            Parameter param = info.parameter();
            IConverter<?> converter = subCommand.getConverter(i);

            if(converter == null) {
                Bukkit.getLogger().warning(String.format("Method %s has a parameter of type %s which does not have a converter", subCommand.getMethod().getName(), param.getType().getName()));
                return Collections.emptyList();
            }

            currentArg += converter.getSize();
            if(currentArg >= context.getArgs().length) {
                TabOverwrite overwrite = info.tabOverwrite();

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
