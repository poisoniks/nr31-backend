package org.nr31.backend.exception;

public class ResourceAccessException extends RuntimeException {
    public ResourceAccessException(String message) {
        super(message);
    }

    public ResourceAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
