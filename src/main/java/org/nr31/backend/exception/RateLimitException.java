package org.nr31.backend.exception;

import lombok.Getter;
import org.nr31.backend.dto.ErrorCode;

import java.util.Map;

@Getter
public class RateLimitException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> metadata;

    public RateLimitException(String message) {
        this(message, ErrorCode.TOO_MANY_REQUESTS, null);
    }

    public RateLimitException(String message, ErrorCode errorCode) {
        this(message, errorCode, null);
    }

    public RateLimitException(String message, Map<String, Object> metadata) {
        this(message, ErrorCode.TOO_MANY_REQUESTS, metadata);
    }

    public RateLimitException(String message, ErrorCode errorCode, Map<String, Object> metadata) {
        super(message);
        this.errorCode = errorCode;
        this.metadata = metadata;
    }
}
