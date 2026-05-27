package nl.xsyntos.scaffi.framework.commands.annotations;

import nl.xsyntos.scaffi.framework.commands.CommandContext;
import nl.xsyntos.scaffi.framework.commands.CommandResponse;

/**
 * A funtional interface that creates a command reponse.
 */
@FunctionalInterface
public interface ICommandResponder {
    public CommandResponse response(CommandContext context);
}
