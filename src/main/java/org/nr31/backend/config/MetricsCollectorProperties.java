package org.nr31.backend.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class MetricsCollectorProperties {
    private final String url;
    private final int timeout;

    public MetricsCollectorProperties(
            @Value("${app.metrics-collector.url:http://metrics-collector:9100}") String url,
            @Value("${app.metrics-collector.timeout:5000}") int timeout) {
        this.url = url;
        this.timeout = timeout;
    }
}
