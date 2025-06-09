package xsyntos.scaffi.framework.commands.annotations;

import xsyntos.scaffi.framework.commands.CommandContext;
import xsyntos.scaffi.framework.commands.CommandResponse;

public class EmptyCommandAsync implements ICommandAsync {

    @Override
    public CommandResponse response(CommandContext context) {
        return CommandResponse.builder()
            .message("Loading...")
            .build();
    }
    
}
