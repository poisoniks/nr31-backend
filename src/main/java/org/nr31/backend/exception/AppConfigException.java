package org.nr31.backend.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AppConfigException extends RuntimeException {
    private String name;

    public AppConfigException(String message) {
        super(message);
    }

    public AppConfigException(String message, String name) {
        super(message);
        this.name = name;
    }
}
