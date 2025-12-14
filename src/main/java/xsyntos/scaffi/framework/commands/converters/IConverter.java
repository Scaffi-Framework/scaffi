package xsyntos.scaffi.framework.commands.converters;

import java.util.List;

import xsyntos.scaffi.framework.commands.CommandContext;
import xsyntos.scaffi.framework.commands.CommandResponse;

/**
 * Converts raw input from Minecraft to Java classes
 */
public interface IConverter<T> {
    /**
     * The actual convert class
     * @param value In case of a multi-param input (size > 1), the different params are seperated by a space (' ')
     * @return
     */
    T convert(CommandContext context, String value);

    /**
     * The amount of Command params it uses for the converter.
     * @return
     */
    int getSize();

    /**
     * The default tab completer for this Java class
     * @param context The raw state of the command
     * @param value The current typed-in value
     * @param currentParam The relative index of Param compared to the size. Starts with 0
     * @return 
     */
    List<String> tabComplete(CommandContext context, String value, int currentParam);
    
    /**
     * Default command response when the converter fails to convert the input to the correct Type.
     * @param value
     * @return
     */
    CommandResponse onError(String value, Exception ex);

    /**
     * Get the type name of the converter for help commands
     * @return
     */
    default String getTypeName() {
        try {
            return this.getClass().getMethod("convert", CommandContext.class, String.class).getReturnType().getSimpleName();
        } catch (NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
            return "Unknown";
        }
    }
}
