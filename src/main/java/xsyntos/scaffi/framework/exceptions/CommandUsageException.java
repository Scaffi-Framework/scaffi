package xsyntos.scaffi.framework.exceptions;

import lombok.Getter;
import xsyntos.scaffi.framework.commands.CommandResponse;

public class CommandUsageException extends RuntimeException {
    @Getter
    private CommandResponse commandResponse;
    public CommandUsageException(String message, CommandResponse commandResponse) {
        super(message);
        this.commandResponse = commandResponse;
    }
}
