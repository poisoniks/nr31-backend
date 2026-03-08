package org.nr31.backend.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class AppConfigValidationException extends RuntimeException {
    private final Map<String, String> errors;

    public AppConfigValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }
}
