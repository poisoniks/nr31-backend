package org.nr31.backend.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.dto.AppConfigDto;
import org.nr31.backend.dto.LogFilesListResponse;
import org.nr31.backend.dto.PermissionDTO;
import org.nr31.backend.dto.PermissionUpdateRequest;
import org.nr31.backend.dto.RoleDTO;
import org.nr31.backend.dto.RoleRequest;
import org.nr31.backend.integration.discord.DiscordBotManager;
import org.nr31.backend.integration.discord.dto.DiscordBotStatusResponse;
import org.nr31.backend.service.AccessControlService;
import org.nr31.backend.service.AppConfigService;
import org.nr31.backend.service.LogService;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Panel", description = "Endpoints for administrative actions")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminPanelController {
    private final CacheManager cacheManager;
    private final EntityManager entityManager;
    private final LogService logService;
    private final AppConfigService appConfigService;
    private final DiscordBotManager discordBotManager;
    private final AccessControlService accessControlService;

    @Operation(summary = "Clear caches", description = "Resets application cache")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully cleared caches")
    })
    @PostMapping(value = "/cache/clear", produces = "application/json")
    @PreAuthorize("hasAuthority('cache:clear')")
    public ResponseEntity<Void> clearCache() {
        log.info("Cache clear issued");
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });

        entityManager.getEntityManagerFactory().getCache().evictAll();

        log.info("Cache cleared successfully");
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List log files", description = "Retrieves a list of available application log files")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved log files list")
    })
    @GetMapping(value = "/logs/list", produces = "application/json")
    @PreAuthorize("hasAuthority('logs:read')")
    public ResponseEntity<LogFilesListResponse> listLogFiles() {
        List<String> fileNames = logService.listLogFiles();
        return ResponseEntity.ok(new LogFilesListResponse(fileNames));
    }

    @Operation(summary = "Get log file", description = "Retrieves the content of a specific log file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved log file"),
            @ApiResponse(responseCode = "403", description = "Invalid path"),
            @ApiResponse(responseCode = "404", description = "Log file not found")
    })
    @GetMapping("/logs/{fileName}")
    @PreAuthorize("hasAuthority('logs:read')")
    public ResponseEntity<Resource> getLogFile(
            @PathVariable String fileName,
            @RequestParam(required = false) Long offsetFromEnd,
            @RequestParam(required = false) Long limit) {
        Resource resource = logService.getLogFile(fileName, offsetFromEnd, limit);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
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

    @Operation(summary = "Start Discord bot", description = "Starts the Discord bot integration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully started the bot")
    })
    @PostMapping("/integrations/discord/start")
    @PreAuthorize("hasAuthority('discord:manage')")
    public ResponseEntity<Void> startBot() {
        discordBotManager.startBot();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Stop Discord bot", description = "Stops the Discord bot integration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully stopped the bot")
    })
    @PostMapping("/integrations/discord/stop")
    @PreAuthorize("hasAuthority('discord:manage')")
    public ResponseEntity<Void> stopBot() {
        discordBotManager.stopBot();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get Discord bot status", description = "Retrieves the current status of the Discord bot")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved bot status")
    })
    @GetMapping("/integrations/discord/status")
    @PreAuthorize("hasAuthority('discord:manage')")
    public ResponseEntity<DiscordBotStatusResponse> getBotStatus() {
        String status = discordBotManager.getBotStatus();
        return ResponseEntity.ok(new DiscordBotStatusResponse(status));
    }

    @Operation(summary = "Assign permission to role", description = "Assigns a specific permission to a role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully assigned permission"),
            @ApiResponse(responseCode = "404", description = "Role or Permission not found")
    })
    @PostMapping("/roles/{roleId}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('access:manage')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignPermissionToRole(@PathVariable Long roleId, @PathVariable Long permissionId) {
        accessControlService.assignPermissionToRole(roleId, permissionId);
    }

    @Operation(summary = "Assign role to user", description = "Assigns a specific role to a user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully assigned role"),
            @ApiResponse(responseCode = "404", description = "User or Role not found")
    })
    @PostMapping("/users/{userId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('access:manage')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignRoleToUser(@PathVariable Long userId, @PathVariable Long roleId) {
        accessControlService.assignRoleToUser(userId, roleId);
    }

    @Operation(summary = "Get all roles", description = "Retrieves a list of all user roles")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved roles list")
    })
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('access:manage')")
    public ResponseEntity<List<RoleDTO>> getAllRoles() {
        return ResponseEntity.ok(accessControlService.getAllRoles());
    }

    @Operation(summary = "Get role by id", description = "Retrieves detailed information about a specific role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved role"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    @GetMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('access:manage')")
    public ResponseEntity<RoleDTO> getRole(@PathVariable Long id) {
        return ResponseEntity.ok(accessControlService.getRole(id));
    }

    @Operation(summary = "Get all permissions", description = "Retrieves a list of all application permissions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved permissions list")
    })
    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('access:manage')")
    public ResponseEntity<List<PermissionDTO>> getAllPermissions() {
        return ResponseEntity.ok(accessControlService.getAllPermissions());
    }

    @Operation(summary = "Update permission description", description = "Updates localized description of a permission")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated permission description"),
            @ApiResponse(responseCode = "404", description = "Permission not found"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload")
    })
    @PutMapping("/permissions/{id}")
    @PreAuthorize("hasAuthority('access:manage')")
    public ResponseEntity<PermissionDTO> updatePermission(
            @PathVariable Long id,
            @Valid @RequestBody PermissionUpdateRequest request) {
        return ResponseEntity.ok(accessControlService.updatePermission(id, request));
    }

    @Operation(summary = "Create role", description = "Creates a new application role")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully created role"),
            @ApiResponse(responseCode = "400", description = "Invalid role data")
    })
    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('access:manage')")
    public ResponseEntity<RoleDTO> createRole(@Valid @RequestBody RoleRequest roleRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accessControlService.createRole(roleRequest));
    }

    @Operation(summary = "Update role", description = "Updates an existing application role's information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated role"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('access:manage')")
    public ResponseEntity<RoleDTO> updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequest roleRequest) {
        return ResponseEntity.ok(accessControlService.updateRole(id, roleRequest));
    }

    @Operation(summary = "Delete role", description = "Permanently removes a role from the application")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted role"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('access:manage')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRole(@PathVariable Long id) {
        accessControlService.deleteRole(id);
    }
}
