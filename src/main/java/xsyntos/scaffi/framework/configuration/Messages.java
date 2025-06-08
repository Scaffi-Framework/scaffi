package xsyntos.scaffi.framework.configuration;

import org.bukkit.ChatColor;
import org.bukkit.Sound;

import lombok.Data;
import xsyntos.scaffi.framework.commands.CommandResponse;

/**
 * A class containing the default messages. These are overwritable
 */
@Data
public class Messages {
    private CommandResponse noPermissions = 
    CommandResponse.builder()
        .message(ChatColor.RED + "You do not have permission to execute this command.")
        .sound(Sound.BLOCK_NOTE_BLOCK_BASS)
        .build();

    private CommandResponse serverError = 
    CommandResponse.builder()
        .message(ChatColor.RED + "An error occurred while executing the command.")
        .sound(Sound.BLOCK_NOTE_BLOCK_BASS)
        .build();

    private CommandResponse invalidUsage =
    CommandResponse.builder()
        .message(ChatColor.RED + "Invalid usage of the command.")
        .sound(Sound.BLOCK_NOTE_BLOCK_BASS)
        .build();


}
