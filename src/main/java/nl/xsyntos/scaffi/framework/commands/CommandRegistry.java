package nl.xsyntos.scaffi.framework.commands;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import org.jetbrains.annotations.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.common.reflect.ClassPath;

import nl.xsyntos.scaffi.framework.commands.annotations.Command;
import nl.xsyntos.scaffi.framework.commands.annotations.HelpCommand;
import nl.xsyntos.scaffi.framework.commands.annotations.SubCommand;
import nl.xsyntos.scaffi.framework.commands.processors.CommandProcessor;
import nl.xsyntos.scaffi.framework.commands.processors.TabProcessor;
import nl.xsyntos.scaffi.framework.exceptions.ScaffiStartupError;

import java.lang.reflect.Parameter;

public class CommandRegistry {
    public static void registerCommands(JavaPlugin plugin) throws IOException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        Class<? extends JavaPlugin> clazz = plugin.getClass();
        ClassPath cp = ClassPath.from(clazz.getClassLoader());

        String packageName = clazz.getPackage().getName();

        for(ClassPath.ClassInfo info : cp.getTopLevelClassesRecursive(packageName)) {
            Class<?> cls = info.load();
            if(cls.isAnnotationPresent(Command.class)) {
                Command command = cls.getAnnotation(Command.class);
                HashMap<String, SubCommandBundle> subCommands = collectSubCommands(command.command(), cls, false);
                PluginCommand plCommand = plugin.getCommand(command.command());
                plCommand.setExecutor(new CommandProcessor(subCommands, command, cls.getDeclaredConstructor().newInstance()));
                plCommand.setTabCompleter(new TabProcessor(subCommands, command));
            }
        }
    }
    
    public static HashMap<String, SubCommandBundle> collectSubCommands(String Command, Class<?> clazz, boolean silent) {
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
                if(!silent)
                    e.printStackTrace();
            }
        }
        
        if(clazz.isAnnotationPresent(HelpCommand.class)) {
            Bukkit.getLogger().info("Registering help command for " + Command);
            subCommands.put("help", generateHelp(Command, clazz.getAnnotation(HelpCommand.class), subCommands));
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

    private static SubCommandBundle generateHelp(String command, HelpCommand helpCommand, HashMap<String, SubCommandBundle> subCommands) {
        SubCommand helpSubCommand = new SubCommand() {
            @Override
            public String command() {
                return "help";
            }

            @Override
            public String description() {
                return "Displays help information";
            }

            @Override
            public String[] aliases() {
                return new String[0];
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return SubCommand.class;
            }

            @Override
            public String permission() {
                return "";
            }

            @Override
            public boolean allowExtraArgs() {
                return false;
            }
        };

        return new HelpCommandBundle(command, helpCommand, helpSubCommand, subCommands);
    }
}

