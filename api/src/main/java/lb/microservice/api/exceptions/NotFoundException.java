package lb.microservice.api.exceptions;

import java.io.Serial;

public class NotFoundException extends RuntimeException {

	@Serial
    private static final long serialVersionUID = 1L;

	public NotFoundException() {}

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundException(Throwable cause) {
        super(cause);
    }
}
