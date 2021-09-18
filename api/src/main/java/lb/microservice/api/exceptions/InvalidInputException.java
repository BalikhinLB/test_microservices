package lb.microservice.api.exceptions;

import java.io.Serial;

public class InvalidInputException extends RuntimeException {

	@Serial
    private static final long serialVersionUID = 1L;

	public InvalidInputException() {}

    public InvalidInputException(String message) {
        super(message);
    }

    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidInputException(Throwable cause) {
        super(cause);
    }
}
