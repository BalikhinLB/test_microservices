package lb.microservice.api.exceptions;

import java.io.Serial;

public class EventProcessingException extends RuntimeException{
    @Serial
    private static final long serialVersionUID = 1L;

    public EventProcessingException() {}

    public EventProcessingException(String message) {
        super(message);
    }

    public EventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventProcessingException(Throwable cause) {
        super(cause);
    }
}
