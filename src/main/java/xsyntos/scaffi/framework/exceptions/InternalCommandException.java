package xsyntos.scaffi.framework.exceptions;

import lombok.Getter;

public class InternalCommandException extends RuntimeException {
    @Getter
    private Exception exception;
    public InternalCommandException(String message, Exception exception) {
        super(message);
        this.exception = exception;
    }

    public InternalCommandException(Exception exception) {
        super(exception.getMessage());
        this.exception = exception;
    }
}
