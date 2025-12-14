package nl.xsyntos.scaffi.framework.commands.converters;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import nl.xsyntos.scaffi.framework.commands.CommandContext;
import nl.xsyntos.scaffi.framework.commands.CommandResponse;
import nl.xsyntos.scaffi.framework.exceptions.UnableConvertException;

/**
 * The default converter for Players
 */
public class PlayerConverter implements IConverter<Player> {
    @Override
    public Player convert(CommandContext context, String value) {
        Player p = Bukkit.getServer().getPlayer(value);
        if (p != null) {
            return p;
        }

        throw new UnableConvertException("Unable to convert " + value + " to player");
    }

    @Override
    public List<String> tabComplete(CommandContext context, String value, int currentParam) {
        return Bukkit.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
    }
    
    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public CommandResponse onError(String value, Exception ex) {
        return CommandResponse.builder()
            .message(value + " is not a valid player!")
            .sound(Sound.BLOCK_NOTE_BLOCK_BASS)
            .build();
    }
}
