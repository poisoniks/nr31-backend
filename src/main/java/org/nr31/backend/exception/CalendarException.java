package org.nr31.backend.exception;

import lombok.Getter;
import org.nr31.backend.dto.common.ErrorCode;

import java.util.Map;

@Getter
public class CalendarException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> metadata;

    public CalendarException(String message) {
        this(message, ErrorCode.INTERNAL_SERVER_ERROR, null);
    }

    public CalendarException(String message, ErrorCode errorCode) {
        this(message, errorCode, null);
    }

    public CalendarException(String message, ErrorCode errorCode, Map<String, Object> metadata) {
        super(message);
        this.errorCode = errorCode;
        this.metadata = metadata;
    }

    public CalendarException(String message, Throwable cause) {
        this(message, ErrorCode.INTERNAL_SERVER_ERROR, null, cause);
    }

    public CalendarException(String message, ErrorCode errorCode, Map<String, Object> metadata, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.metadata = metadata;
    }

    public static class UserError extends CalendarException {
        public UserError(String message) {
            super(message, ErrorCode.VALIDATION_ERROR, null);
        }

        public UserError(String message, ErrorCode errorCode) {
            super(message, errorCode, null);
        }

        public UserError(String message, ErrorCode errorCode, Map<String, Object> metadata) {
            super(message, errorCode, metadata);
        }
    }

    public static class ServerError extends CalendarException {
        public ServerError(String message) {
            super(message, ErrorCode.INTERNAL_SERVER_ERROR, null);
        }

        public ServerError(String message, Throwable cause) {
            super(message, ErrorCode.INTERNAL_SERVER_ERROR, null, cause);
        }
    }
}
