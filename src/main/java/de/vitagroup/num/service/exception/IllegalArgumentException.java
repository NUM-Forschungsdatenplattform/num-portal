package de.vitagroup.num.service.exception;

public class IllegalArgumentException extends RuntimeException {

    public IllegalArgumentException() {
        super();
    }

    public IllegalArgumentException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalArgumentException(String message) {
        super(message);
    }

    public IllegalArgumentException(Throwable cause) {
        super(cause);
    }
}