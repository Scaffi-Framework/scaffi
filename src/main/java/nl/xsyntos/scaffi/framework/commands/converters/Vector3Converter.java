package nl.xsyntos.scaffi.framework.commands.converters;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.block.CommandBlock;
import org.bukkit.entity.Entity;

import nl.xsyntos.scaffi.framework.commands.CommandContext;
import nl.xsyntos.scaffi.framework.commands.CommandResponse;
import nl.xsyntos.scaffi.framework.configuration.Vector3;
import nl.xsyntos.scaffi.framework.exceptions.UnableConvertException;

/**
 * The default converter for Vector3's
 */
public class Vector3Converter implements IConverter<Vector3> {

    @Override
    public Vector3 convert(CommandContext context, String value) {
        String[] values = value.split(" ");
        if(values.length != 3)
            throw new UnableConvertException("Unable to convert " + value + " to a blockpos");
        
        try {
            Double x = Double.parseDouble(values[0]);
            Double y = Double.parseDouble(values[1]);
            Double z = Double.parseDouble(values[2]);

            return new Vector3(x, y, z);
        } catch(NumberFormatException  ex) {
            throw new UnableConvertException("Unable to convert " + value + " to a blockpos");
        }

    }

    @Override
    public int getSize() {
        return 3;
    }

    @Override
    public List<String> tabComplete(CommandContext context, String value, int currentParam) {
        if(context.getSender() instanceof CommandBlock) {
            return List.of("~");
        } else if(context.getSender() instanceof Entity player) {
            switch (currentParam) {
                case 0:
                    return List.of(
                            String.format(Locale.US, "%.2f", player.getLocation().getX())
                        );
                case 1:
                    return List.of(
                            String.format(Locale.US, "%.2f", player.getLocation().getY())
                        );
                case 2:
                    return List.of(
                        String.format(Locale.US, "%.2f", player.getLocation().getZ())
                    );
                default:
                    break;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public CommandResponse onError(String value, Exception ex) {
        return CommandResponse.builder()
            .message(ChatColor.RED + value + " is not a valid blockpos!")
            .sound(Sound.BLOCK_NOTE_BLOCK_BASS)
            .build();
    }
    
}
