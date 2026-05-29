package org.nr31.backend.service.impl;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.config.MetricsCollectorProperties;
import org.nr31.backend.exception.ServiceUnavailableException;
import org.nr31.backend.service.HostMetricsService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Service
public class HostMetricsServiceImpl implements HostMetricsService {

    private final MetricsCollectorProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ExecutorService executorService;

    @Autowired
    public HostMetricsServiceImpl(MetricsCollectorProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getTimeout()))
                .build());
    }

    public HostMetricsServiceImpl(MetricsCollectorProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "metrics-stream-consumer");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public JsonNode getMetrics() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getUrl() + "/metrics"))
                .timeout(Duration.ofMillis(properties.getTimeout()))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ServiceUnavailableException("Metrics collector returned status: " + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (IOException e) {
            log.error("Failed to fetch metrics: network or parsing error", e);
            throw new ServiceUnavailableException("Metrics collector is unreachable: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            log.error("Metrics fetch interrupted", e);
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException("Metrics request interrupted", e);
        }
    }

    @Override
    public JsonNode getHistory() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getUrl() + "/metrics/history"))
                .timeout(Duration.ofMillis(properties.getTimeout()))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ServiceUnavailableException("Metrics collector history returned status: " + response.statusCode());
            }
            return objectMapper.readTree(response.body());
        } catch (IOException e) {
            log.error("Failed to fetch metrics history: network or parsing error", e);
            throw new ServiceUnavailableException("Metrics collector history is unreachable: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            log.error("Metrics history fetch interrupted", e);
            Thread.currentThread().interrupt();
            throw new ServiceUnavailableException("Metrics history request interrupted", e);
        }
    }

    @Override
    public SseEmitter streamMetrics() {
        SseEmitter emitter = createSseEmitter();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(properties.getUrl() + "/metrics/stream"))
                .GET()
                .build();

        executorService.submit(() -> {
            try {
                HttpResponse<Stream<String>> response = httpClient.send(request, HttpResponse.BodyHandlers.ofLines());
                if (response.statusCode() >= 400) {
                    emitter.completeWithError(new ServiceUnavailableException("Metrics stream returned status: " + response.statusCode()));
                    return;
                }
                try (Stream<String> lines = response.body()) {
                    lines.forEach(line -> {
                        try {
                            if (line.startsWith("data:")) {
                                String data = line.substring(5).trim();
                                if (!data.isEmpty()) {
                                    emitter.send(SseEmitter.event()
                                            .data(data, MediaType.APPLICATION_JSON));
                                }
                            } else if (!line.startsWith(":") && !line.trim().isEmpty()) {
                                String trimmed = line.trim();
                                if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                                    emitter.send(SseEmitter.event()
                                            .data(trimmed, MediaType.APPLICATION_JSON));
                                }
                            }
                        } catch (Exception e) {
                            log.debug("SSE emitter connection closed by client or error occurred", e);
                            throw new RuntimeException("Stop consuming stream", e);
                        }
                    });
                }
                emitter.complete();
            } catch (IOException e) {
                log.error("Metrics stream IO exception", e);
                emitter.completeWithError(new ServiceUnavailableException("Metrics stream connection failed: " + e.getMessage(), e));
            } catch (InterruptedException e) {
                log.error("Metrics stream connection interrupted", e);
                Thread.currentThread().interrupt();
                emitter.complete();
            } catch (Exception e) {
                // Caught RuntimeException from inside forEach to stop loop
                emitter.complete();
            }
        });

        return emitter;
    }

    protected SseEmitter createSseEmitter() {
        return new SseEmitter(0L);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down HostMetricsServiceImpl executor service...");
        executorService.shutdownNow();
    }
}
