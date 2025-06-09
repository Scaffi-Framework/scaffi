package xsyntos.scaffi.framework.commands.annotations;

import xsyntos.scaffi.framework.commands.CommandContext;
import xsyntos.scaffi.framework.commands.CommandResponse;

/**
 * A Function that returns the temporary CommandResponse while the command is executing
 */
@FunctionalInterface
public interface ICommandAsync {
    public CommandResponse response(CommandContext context);
}
