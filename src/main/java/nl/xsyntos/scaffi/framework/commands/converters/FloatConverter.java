package nl.xsyntos.scaffi.framework.commands.converters;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Sound;

import nl.xsyntos.scaffi.framework.commands.CommandContext;
import nl.xsyntos.scaffi.framework.commands.CommandResponse;
import nl.xsyntos.scaffi.framework.exceptions.UnableConvertException;

import java.util.Collections;

/**
 * The default converter for Float
 */
public class FloatConverter implements IConverter<Float> {
    @Override
    public Float convert(CommandContext context, String value) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw new UnableConvertException("Unable to convert " + value + " to int");
        }
    }

    @Override
    public List<String> tabComplete(CommandContext context, String value, int currentParam) {
        return Collections.emptyList();
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public CommandResponse onError(String value, Exception ex) {
        return CommandResponse.builder()
            .message(ChatColor.RED + value + " is not a valid number!")
            .sound(Sound.BLOCK_NOTE_BLOCK_BASS)
            .build();
    }
}
