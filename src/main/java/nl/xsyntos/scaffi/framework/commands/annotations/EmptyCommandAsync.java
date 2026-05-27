package nl.xsyntos.scaffi.framework.commands.annotations;

import nl.xsyntos.scaffi.framework.commands.CommandContext;
import nl.xsyntos.scaffi.framework.commands.CommandResponse;

public class EmptyCommandAsync implements ICommandResponder {

    @Override
    public CommandResponse response(CommandContext context) {
        return CommandResponse.builder()
            .message("Loading...")
            .build();
    }
    
}
