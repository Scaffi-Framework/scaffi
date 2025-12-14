package nl.xsyntos.scaffi.framework.commands;

import org.bukkit.command.CommandSender;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Context of the command (the raw params of the default Bukkit command function)
 */
@Data
@AllArgsConstructor
public class CommandContext {
    /**
     * Who sends the command
     */
    private CommandSender sender;

    /**
     * All arguments
     */
    private String[] args;

    /**
     * What alias is used
     */
    private String alias;
    // private Command command;
}
