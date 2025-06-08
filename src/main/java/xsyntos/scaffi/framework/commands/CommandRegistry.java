package xsyntos.scaffi.framework.commands;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import javax.annotation.Nullable;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.common.reflect.ClassPath;
import java.lang.reflect.Parameter;

import xsyntos.scaffi.framework.exceptions.ScaffiStartupError;

public class CommandRegistry {
    public static void registerCommands(JavaPlugin plugin) throws IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        Class<? extends JavaPlugin> clazz = plugin.getClass();
        ClassPath cp = ClassPath.from(clazz.getClassLoader());

        String packageName = clazz.getPackage().getName();

        for(ClassPath.ClassInfo info : cp.getTopLevelClassesRecursive(packageName)) {
            Class<?> cls = info.load();
            if(cls.isAnnotationPresent(Command.class)) {
                Command command = cls.getAnnotation(Command.class);
                HashMap<String, SubCommandBundle> subCommands = collectSubCommands(cls);
                PluginCommand plCommand = plugin.getCommand(command.command());
                plCommand.setExecutor(new CommandProcessor(subCommands, command, cls.getDeclaredConstructor().newInstance()));
                plCommand.setTabCompleter(new TabProcessor(subCommands, command));
            }
        }
    }
    
    public static HashMap<String, SubCommandBundle> collectSubCommands(Class<?> clazz) {
        HashMap<String, SubCommandBundle> subCommands = new HashMap<>();

        for(Method method : clazz.getMethods()) {
            try {
                if(method.isAnnotationPresent(SubCommand.class)) {
                    validateMethod(method);

                    SubCommand subCommand = method.getAnnotation(SubCommand.class);
                    if(!subCommands.containsKey(subCommand.command())) 
                        subCommands.put(subCommand.command(), new SubCommandBundle(subCommand, method));
                    else
                        throw new ScaffiStartupError(String.format("Duplicate subcommand %s in class %s", subCommand.command(), clazz.getName()));
                    for(String alias : subCommand.aliases()) {
                        if(!subCommands.containsKey(alias)) 
                            subCommands.put(alias, new SubCommandBundle(subCommand, method));
                        else
                            throw new ScaffiStartupError(String.format("Duplicate subcommand %s in class %s", alias, clazz.getName()));
                    }
                }

            } catch (ScaffiStartupError e) {
                e.printStackTrace();
            }
        }
        return subCommands;
    }


    private static void validateMethod(Method method) throws ScaffiStartupError {
        if(!method.getReturnType().equals(CommandResponse.class)) {
            throw new ScaffiStartupError(String.format("Method %s in Class %s does not return CommandResponse", method.getName(), method.getDeclaringClass().getName()));
        } else if(method.getParameterCount() == 0) {
            throw new ScaffiStartupError(String.format("Method %s in Class %s does not have any parameters", method.getName(), method.getDeclaringClass().getName()));
        } else if (!method.getParameterTypes()[0].equals(CommandContext.class)) {
            throw new ScaffiStartupError(String.format("Method %s in Class %s does not have CommandContext as first parameter type", method.getName(), method.getDeclaringClass().getName()));
        }
        
        boolean foundTheFirstNullable = false;

        //Loop through all parameters and check if they have a nullable annotation
        for(int i = 1; i < method.getParameters().length; i++) {
            Parameter parameter = method.getParameters()[i];
            if(ConverterRegistry.getConverter(parameter.getType()) == null) {
                throw new ScaffiStartupError(String.format("Method %s in Class %s has a parameter of type %s which does not have a converter", method.getName(), method.getDeclaringClass().getName(), parameter.getType().getName()));
            }

            if(parameter.isAnnotationPresent(Nullable.class)) {
                foundTheFirstNullable = true;
            } else {
                if(foundTheFirstNullable) {
                    throw new ScaffiStartupError(String.format("Method %s in Class %s has a parameter that's not nullable after a nullable parameter", method.getName(), method.getDeclaringClass().getName()));
                }
            }

        }
    }
    
}

