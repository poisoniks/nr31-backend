package org.nr31.backend.controller.v1;

import tools.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.service.HostMetricsService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/host-metrics")
@RequiredArgsConstructor
@Tag(name = "Host Metrics", description = "Endpoints for proxying host performance and container metrics")
@SecurityRequirement(name = "Bearer Authentication")
public class HostMetricsController {

    private final HostMetricsService hostMetricsService;

    @Operation(summary = "Get current host metrics", description = "Proxies current system performance metrics from metrics collector sidecar")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved metrics"),
            @ApiResponse(responseCode = "503", description = "Metrics collector service is unavailable")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('admin:view')")
    public ResponseEntity<JsonNode> getMetrics() {
        return ResponseEntity.ok(hostMetricsService.getMetrics());
    }

    @Operation(summary = "Get host metrics history", description = "Proxies recorded historical system metrics from metrics collector sidecar")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved metrics history"),
            @ApiResponse(responseCode = "503", description = "Metrics collector service is unavailable")
    })
    @GetMapping(value = "/history", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('admin:view')")
    public ResponseEntity<JsonNode> getHistory() {
        return ResponseEntity.ok(hostMetricsService.getHistory());
    }

    @Operation(summary = "Stream host metrics via SSE", description = "Opens an SSE stream to proxy real-time performance updates")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully opened SSE stream"),
            @ApiResponse(responseCode = "503", description = "Metrics collector service is unavailable")
    })
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('admin:view')")
    public SseEmitter streamMetrics() {
        return hostMetricsService.streamMetrics();
    }
}
