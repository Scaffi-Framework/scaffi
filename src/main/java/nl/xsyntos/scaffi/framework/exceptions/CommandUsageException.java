package nl.xsyntos.scaffi.framework.exceptions;

import lombok.Getter;
import nl.xsyntos.scaffi.framework.commands.CommandResponse;

public class CommandUsageException extends RuntimeException {
    @Getter
    private CommandResponse commandResponse;
    public CommandUsageException(String message, CommandResponse commandResponse) {
        super(message);
        this.commandResponse = commandResponse;
    }
}
