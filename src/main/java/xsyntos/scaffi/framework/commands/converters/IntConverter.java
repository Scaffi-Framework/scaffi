package xsyntos.scaffi.framework.commands.converters;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Sound;

import java.util.Collections;

import xsyntos.scaffi.framework.commands.CommandContext;
import xsyntos.scaffi.framework.commands.CommandResponse;
import xsyntos.scaffi.framework.commands.IConverter;
import xsyntos.scaffi.framework.exceptions.UnableConvertException;

/**
 * The default converter for Integers
 */
public class IntConverter implements IConverter<Integer> {
    @Override
    public Integer convert(String value) {
        try {
            return Integer.parseInt(value);
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
    public CommandResponse onError(String value) {
        return CommandResponse.builder()
            .message(ChatColor.RED + value + " is not a valid number!")
            .sound(Sound.BLOCK_NOTE_BLOCK_BASS)
            .build();
    }
}
