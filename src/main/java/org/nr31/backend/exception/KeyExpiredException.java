package org.nr31.backend.exception;

import lombok.Getter;
import org.nr31.backend.dto.common.ErrorCode;

import java.util.Map;

@Getter
public class KeyExpiredException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> metadata;

    public KeyExpiredException(String message) {
        this(message, ErrorCode.VALIDATION_ERROR, null);
    }

    public KeyExpiredException(String message, ErrorCode errorCode) {
        this(message, errorCode, null);
    }

    public KeyExpiredException(String message, ErrorCode errorCode, Map<String, Object> metadata) {
        super(message);
        this.errorCode = errorCode;
        this.metadata = metadata;
    }
}
