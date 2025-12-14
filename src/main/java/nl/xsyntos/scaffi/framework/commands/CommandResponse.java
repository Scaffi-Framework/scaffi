package nl.xsyntos.scaffi.framework.commands;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import lombok.Builder;
import lombok.Data;
import net.md_5.bungee.api.chat.BaseComponent;

/**
 * The expected result of a Scaffi-based Minecraft Command
 */
@Data
@Builder
public class CommandResponse {
    /**
     * The message that will be send to the commandsender after the command completion
     * If it's null no message will be send
     */
    private String message;

    /**
     * The sound that will be played to the commandsender after the command completion
     * If it's null no sound will be played
     */
    private Sound sound;

    private BaseComponent component;

    public void send(CommandContext context) {
        if (message != null) 
            context.getSender().sendMessage(message);
        
        if(component != null) 
            context.getSender().spigot().sendMessage(component);
        

        if(context.getSender() instanceof Player player) {
            if(sound != null) {
                player.playSound(player.getLocation(), sound, 1, 1);
            }
        }
    }
}
