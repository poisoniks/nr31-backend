package org.nr31.backend.service.impl;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nr31.backend.config.MetricsCollectorProperties;
import org.nr31.backend.exception.ServiceUnavailableException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HostMetricsServiceImplTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> stringResponse;

    @Mock
    private HttpResponse<Stream<String>> streamResponse;

    private HostMetricsServiceImpl hostMetricsService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MetricsCollectorProperties properties = new MetricsCollectorProperties("http://localhost:9100", 5000);

    @BeforeEach
    void setUp() {
        hostMetricsService = spy(new HostMetricsServiceImpl(properties, objectMapper, httpClient));
    }

    @Test
    void getMetrics_Success() throws Exception {
        String json = "{\"host\":{\"cpuUsagePercent\":0.5}}";
        when(stringResponse.statusCode()).thenReturn(200);
        when(stringResponse.body()).thenReturn(json);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(stringResponse);

        JsonNode result = hostMetricsService.getMetrics();

        assertNotNull(result);
        assertEquals(0.5, result.get("host").get("cpuUsagePercent").asDouble());
    }

    @Test
    void getMetrics_HttpError() throws Exception {
        when(stringResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(stringResponse);

        assertThrows(ServiceUnavailableException.class, () -> hostMetricsService.getMetrics());
    }

    @Test
    void getMetrics_NetworkError() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("Connection reset"));

        assertThrows(ServiceUnavailableException.class, () -> hostMetricsService.getMetrics());
    }

    @Test
    void getHistory_Success() throws Exception {
        String json = "[{\"collectedAt\":\"2026-05-29\"}]";
        when(stringResponse.statusCode()).thenReturn(200);
        when(stringResponse.body()).thenReturn(json);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(stringResponse);

        JsonNode result = hostMetricsService.getHistory();

        assertNotNull(result);
        assertTrue(result.isArray());
        assertEquals("2026-05-29", result.get(0).get("collectedAt").asText());
    }

    @Test
    void getHistory_HttpError() throws Exception {
        when(stringResponse.statusCode()).thenReturn(404);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(stringResponse);

        assertThrows(ServiceUnavailableException.class, () -> hostMetricsService.getHistory());
    }

    @Test
    void streamMetrics_Success() throws Exception {
        Stream<String> mockStream = Stream.of(
                "data: {\"cpu\":0.1}",
                "",
                "data: {\"cpu\":0.2}"
        );
        when(streamResponse.statusCode()).thenReturn(200);
        when(streamResponse.body()).thenReturn(mockStream);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(streamResponse);

        SseEmitter mockEmitter = mock(SseEmitter.class);
        doReturn(mockEmitter).when(hostMetricsService).createSseEmitter();

        SseEmitter returnedEmitter = hostMetricsService.streamMetrics();
        assertSame(mockEmitter, returnedEmitter);

        // Verify send and complete are called on mockEmitter asynchronously
        verify(mockEmitter, timeout(5000).times(2)).send(any(SseEmitter.SseEventBuilder.class));
        verify(mockEmitter, timeout(5000)).complete();
    }

    @Test
    void streamMetrics_HttpError() throws Exception {
        when(streamResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(streamResponse);

        SseEmitter mockEmitter = mock(SseEmitter.class);
        doReturn(mockEmitter).when(hostMetricsService).createSseEmitter();

        SseEmitter returnedEmitter = hostMetricsService.streamMetrics();
        assertSame(mockEmitter, returnedEmitter);

        // Verify completeWithError is called on mockEmitter asynchronously
        verify(mockEmitter, timeout(5000)).completeWithError(any(ServiceUnavailableException.class));
    }
}
