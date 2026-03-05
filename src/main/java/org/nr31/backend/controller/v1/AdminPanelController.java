package org.nr31.backend.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Panel", description = "Endpoints for administrative actions")
public class AdminPanelController {

    private final CacheManager cacheManager;
    private final EntityManager entityManager;

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
}
