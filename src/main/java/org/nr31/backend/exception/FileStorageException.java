package org.nr31.backend.exception;

import lombok.Getter;
import org.nr31.backend.dto.ErrorCode;

import java.util.Map;

@Getter
public class FileStorageException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> metadata;

    public FileStorageException(String message) {
        this(message, ErrorCode.STORAGE_ERROR, null);
    }

    public FileStorageException(String message, ErrorCode errorCode) {
        this(message, errorCode, null);
    }

    public FileStorageException(String message, ErrorCode errorCode, Map<String, Object> metadata) {
        super(message);
        this.errorCode = errorCode;
        this.metadata = metadata;
    }

    public FileStorageException(String message, Throwable cause) {
        this(message, ErrorCode.STORAGE_ERROR, null, cause);
    }

    public FileStorageException(String message, ErrorCode errorCode, Map<String, Object> metadata, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.metadata = metadata;
    }
}
