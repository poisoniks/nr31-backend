package org.nr31.backend.exception;

import lombok.Getter;
import org.nr31.backend.dto.ErrorCode;

import java.util.Map;

@Getter
public class ConflictException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> metadata;

    public ConflictException(String message) {
        this(message, ErrorCode.CONFLICT, null);
    }

    public ConflictException(String message, ErrorCode errorCode) {
        this(message, errorCode, null);
    }

    public ConflictException(String message, Map<String, Object> metadata) {
        this(message, ErrorCode.CONFLICT, metadata);
    }

    public ConflictException(String message, ErrorCode errorCode, Map<String, Object> metadata) {
        super(message);
        this.errorCode = errorCode;
        this.metadata = metadata;
    }
}
