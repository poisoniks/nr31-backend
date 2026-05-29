package org.nr31.backend.service;

import tools.jackson.databind.JsonNode;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface HostMetricsService {
    JsonNode getMetrics();
    JsonNode getHistory();
    SseEmitter streamMetrics();
}
