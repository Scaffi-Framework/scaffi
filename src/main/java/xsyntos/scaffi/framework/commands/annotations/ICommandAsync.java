package xsyntos.scaffi.framework.commands.annotations;

import xsyntos.scaffi.framework.commands.CommandContext;
import xsyntos.scaffi.framework.commands.CommandResponse;

@FunctionalInterface
public interface ICommandAsync {
    public CommandResponse response(CommandContext context);
}
