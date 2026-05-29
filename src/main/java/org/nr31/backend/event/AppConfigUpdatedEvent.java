package org.nr31.backend.event;

import lombok.Getter;
import org.nr31.backend.model.AppConfig;
import org.springframework.context.ApplicationEvent;

@Getter
public class AppConfigUpdatedEvent extends ApplicationEvent {
    private final AppConfig appConfig;

    public AppConfigUpdatedEvent(Object source, AppConfig appConfig) {
        super(source);
        this.appConfig = appConfig;
    }
}
