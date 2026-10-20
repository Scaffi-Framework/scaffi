package nl.xsyntos.scaffi.framework.commands.annotations;

import nl.xsyntos.scaffi.framework.ScaffiPlugin;
import nl.xsyntos.scaffi.framework.commands.CommandContext;
import nl.xsyntos.scaffi.framework.commands.CommandResponse;

public class DefaultErrorResponder implements ICommandResponder {

    @Override
    public CommandResponse response(CommandContext context) {
        return ScaffiPlugin.config.getMessages().getServerError();
    }
    
}
