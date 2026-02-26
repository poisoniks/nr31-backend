package org.nr31.backend.exception;

public class CalendarException extends RuntimeException {
    public CalendarException(String message, Throwable cause) {
        super(message, cause);
    }

    public CalendarException(String message) {
        super(message);
    }

    public static class UserError extends CalendarException {

        public UserError(String message, Throwable cause) {
            super(message, cause);
        }

        public UserError(String message) {
            super(message);
        }
    }

    public static class ServerError extends CalendarException {

        public ServerError(String message, Throwable cause) {
            super(message, cause);
        }

        public ServerError(String message) {
            super(message);
        }
    }
}
