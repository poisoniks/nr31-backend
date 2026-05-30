package org.nr31.backend.controller.v1;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nr31.backend.service.HostMetricsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HostMetricsControllerTest {

    @Mock
    private HostMetricsService hostMetricsService;

    private HostMetricsController hostMetricsController;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        hostMetricsController = new HostMetricsController(hostMetricsService);
    }

    @Test
    void shouldReturnMetrics() {
        JsonNode mockNode = objectMapper.readTree("{\"cpu\": 0.5}");
        when(hostMetricsService.getMetrics()).thenReturn(mockNode);

        ResponseEntity<JsonNode> response = hostMetricsController.getMetrics();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockNode, response.getBody());
        verify(hostMetricsService).getMetrics();
    }

    @Test
    void shouldReturnHistory() {
        JsonNode mockNode = objectMapper.readTree("[{\"cpu\": 0.5}]");
        when(hostMetricsService.getHistory()).thenReturn(mockNode);

        ResponseEntity<JsonNode> response = hostMetricsController.getHistory();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockNode, response.getBody());
        verify(hostMetricsService).getHistory();
    }

    @Test
    void shouldReturnSseEmitter() {
        SseEmitter mockEmitter = new SseEmitter();
        when(hostMetricsService.streamMetrics()).thenReturn(mockEmitter);

        SseEmitter response = hostMetricsController.streamMetrics();

        assertNotNull(response);
        assertEquals(mockEmitter, response);
        verify(hostMetricsService).streamMetrics();
    }
}
