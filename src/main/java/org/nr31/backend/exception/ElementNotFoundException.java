package org.nr31.backend.exception;

import lombok.Getter;
import org.nr31.backend.dto.common.ErrorCode;

import java.util.Map;

@Getter
public class ElementNotFoundException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> metadata;

    public ElementNotFoundException(String message) {
        this(message, ErrorCode.ELEMENT_NOT_FOUND, null);
    }

    public ElementNotFoundException(String message, ErrorCode errorCode) {
        this(message, errorCode, null);
    }

    public ElementNotFoundException(String message, Map<String, Object> metadata) {
        this(message, ErrorCode.ELEMENT_NOT_FOUND, metadata);
    }

    public ElementNotFoundException(String message, ErrorCode errorCode, Map<String, Object> metadata) {
        super(message);
        this.errorCode = errorCode;
        this.metadata = metadata;
    }
}
