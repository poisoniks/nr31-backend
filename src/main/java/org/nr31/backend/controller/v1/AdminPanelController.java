package org.nr31.backend.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.AppConfigDto;
import org.nr31.backend.service.AppConfigService;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Panel", description = "Endpoints for administrative actions")
public class AdminPanelController {

    private final CacheManager cacheManager;
    private final EntityManager entityManager;
    private final AppConfigService appConfigService;

    @Operation(summary = "Clear caches", description = "Resets application cache")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully cleared caches")
    })
    @PostMapping(value = "/cache/clear", produces = "application/json")
    @PreAuthorize("hasAuthority('cache:clear')")
    public ResponseEntity<Void> clearCache() {
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });

        entityManager.getEntityManagerFactory().getCache().evictAll();

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get application config", description = "Retrieves an application configuration by key")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved config"),
            @ApiResponse(responseCode = "404", description = "Config not found")
    })
    @GetMapping("/config/{name}")
    @PreAuthorize("hasAuthority('config:read')")
    public ResponseEntity<AppConfigDto> getConfig(@PathVariable String name) {
        return ResponseEntity.ok(appConfigService.getConfig(name));
    }

    @Operation(summary = "Get all application configs", description = "Retrieves all application configurations with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved configs")
    })
    @GetMapping("/config")
    @PreAuthorize("hasAuthority('config:read')")
    public ResponseEntity<Page<AppConfigDto>> getAllConfigs(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(appConfigService.getAllConfigs(pageable));
    }

    @Operation(summary = "Update application config", description = "Updates or creates an application configuration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated config"),
            @ApiResponse(responseCode = "400", description = "Invalid configuration data")
    })
    @PutMapping("/config/{name}")
    @PreAuthorize("hasAuthority('config:write')")
    public ResponseEntity<AppConfigDto> updateConfig(
            @PathVariable String name,
            @Valid @RequestBody AppConfigDto appConfigDto) {
        return ResponseEntity.ok(appConfigService.updateConfig(name, appConfigDto));
    }
}
