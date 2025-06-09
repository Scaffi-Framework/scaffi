package xsyntos.scaffi.framework.commands.converters;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Sound;

import xsyntos.scaffi.framework.commands.CommandContext;
import xsyntos.scaffi.framework.commands.CommandResponse;


/**
 * The default converter for Booleans
 */
public class BooleanConverter implements IConverter<Boolean> {
    @Override
    public Boolean convert(CommandContext context, String value) {
        return Boolean.parseBoolean(value);
    }

    @Override
    public List<String> tabComplete(CommandContext context, String value, int currentParam) {
        return List.of("true", "false");
    }
    
    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public CommandResponse onError(String value, Exception ex) {
        return CommandResponse.builder()
            .message(ChatColor.RED + value + " is not a valid boolean!")
            .sound(Sound.BLOCK_NOTE_BLOCK_BASS)
            .build();
    }
}
