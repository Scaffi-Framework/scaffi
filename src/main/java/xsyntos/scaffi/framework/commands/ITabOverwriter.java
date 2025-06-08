package xsyntos.scaffi.framework.commands;

import java.util.List;

/**
 * The input for the TabOverwrite annotation
 */
@FunctionalInterface
public interface ITabOverwriter {
    /**
     * The custom taboverwriter for the associated param
     * @param context The raw state of the command
     * @param value The current typed-in value
     * @param currentParam The relative index of Param compared to the size. Starts with 0
     * @return 
     */
    List<String> tabComplete(CommandContext context, String value, int currentParam);
}
