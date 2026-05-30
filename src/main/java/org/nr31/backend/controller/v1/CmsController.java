package org.nr31.backend.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.integration.DiscordWidgetDataDto;
import org.nr31.backend.dto.integration.YoutubeVideoDto;
import org.nr31.backend.dto.cms.PageResponseDto;
import org.nr31.backend.dto.cms.PublishDraftRequest;
import org.nr31.backend.dto.cms.SlotRestrictionsDto;
import org.nr31.backend.dto.cms.UpdateDraftRequest;
import org.nr31.backend.dto.cms.UpdateSlotRestrictionsRequest;
import org.nr31.backend.service.CmsService;
import org.nr31.backend.service.DiscordWidgetService;
import org.nr31.backend.service.ValidationService;
import org.nr31.backend.service.YouTubeService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import tools.jackson.databind.JsonNode;
import org.nr31.backend.service.WidgetSchemaService;

@RestController
@RequestMapping("/api/v1/cms")
@RequiredArgsConstructor
@Tag(name = "CMS", description = "Content Management System endpoints for managing pages and slot restrictions")
public class CmsController {

    private final CmsService cmsService;
    private final ValidationService validationService;
    private final YouTubeService youTubeService;
    private final DiscordWidgetService discordWidgetService;
    private final WidgetSchemaService widgetSchemaService;

    @Operation(summary = "Get published page by slug", description = "Retrieves the published revision of a page by its slug")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved published page"),
            @ApiResponse(responseCode = "404", description = "Page not found or no published revision exists")
    })
    @GetMapping(value = "/pages/{slug}", produces = "application/json")
    @Cacheable(value = "publishedPages", key = "#slug")
    public ResponseEntity<PageResponseDto> getPublishedPage(
            @Parameter(description = "URL-friendly page identifier", example = "home")
            @PathVariable String slug) {
        return ResponseEntity.ok(cmsService.getPublishedPage(slug));
    }

    @Operation(summary = "Get draft page by slug", description = "Retrieves the draft revision of a page. If no draft exists, duplicates the published revision as a new draft.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved draft page"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "403", description = "User lacks required cms:write permission"),
            @ApiResponse(responseCode = "404", description = "Page not found")
    })
    @GetMapping(value = "/pages/{slug}/draft", produces = "application/json")
    @PreAuthorize("hasAuthority('cms:write')")
    public ResponseEntity<PageResponseDto> getDraftPage(
            @Parameter(description = "URL-friendly page identifier", example = "home")
            @PathVariable String slug) {
        return ResponseEntity.ok(cmsService.getDraftPage(slug));
    }

    @Operation(summary = "Update draft page", description = "Updates the draft revision with new layout data. Validates version for optimistic locking and layout against slot restrictions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated draft page"),
            @ApiResponse(responseCode = "400", description = "Invalid request body or layout validation failed"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "403", description = "User lacks required cms:write permission"),
            @ApiResponse(responseCode = "404", description = "Page not found"),
            @ApiResponse(responseCode = "409", description = "Version conflict - page was modified by another user")
    })
    @PutMapping(value = "/pages/{slug}/draft", produces = "application/json")
    @PreAuthorize("hasAuthority('cms:write')")
    public ResponseEntity<PageResponseDto> updateDraft(
            @Parameter(description = "URL-friendly page identifier", example = "home")
            @PathVariable String slug,
            @Valid @RequestBody UpdateDraftRequest request) {
        return ResponseEntity.ok(cmsService.updateDraft(slug, request));
    }

    @Operation(summary = "Publish draft page", description = "Publishes a draft revision, archiving the current published revision if it exists. Increments the page version number.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully published draft page"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "403", description = "User lacks required cms:write permission"),
            @ApiResponse(responseCode = "404", description = "Page or draft revision not found"),
            @ApiResponse(responseCode = "409", description = "Version conflict - page was modified by another user")
    })
    @PostMapping(value = "/pages/{slug}/publish", produces = "application/json")
    @PreAuthorize("hasAuthority('cms:write')")
    @CacheEvict(value = "publishedPages", key = "#slug")
    public ResponseEntity<PageResponseDto> publishDraft(
            @Parameter(description = "URL-friendly page identifier", example = "home")
            @PathVariable String slug,
            @Valid @RequestBody PublishDraftRequest request) {
        return ResponseEntity.ok(cmsService.publishDraft(slug, request));
    }

    @Operation(summary = "Get slot restrictions", description = "Retrieves the current slot restriction configuration defining which widget types are allowed in each slot type.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved slot restrictions"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "403", description = "User lacks required cms:write permission")
    })
    @GetMapping(value = "/slot-restrictions", produces = "application/json")
    @PreAuthorize("hasAuthority('cms:write')")
    public ResponseEntity<SlotRestrictionsDto> getSlotRestrictions() {
        return ResponseEntity.ok(validationService.getSlotRestrictions());
    }

    @Operation(summary = "Update slot restrictions", description = "Updates the slot restriction configuration. Validates the JSON structure and evicts the cache.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated slot restrictions"),
            @ApiResponse(responseCode = "400", description = "Invalid request body or slot restriction structure"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "403", description = "User lacks required cms:write permission")
    })
    @PutMapping(value = "/slot-restrictions", produces = "application/json")
    @PreAuthorize("hasAuthority('cms:write')")
    public ResponseEntity<SlotRestrictionsDto> updateSlotRestrictions(@Valid @RequestBody UpdateSlotRestrictionsRequest request) {
        validationService.updateSlotRestrictions(request);
        SlotRestrictionsDto updated = validationService.getSlotRestrictions();
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Get latest YouTube video", description = "Returns the cached latest video for a given YouTube channel ID. Data is refreshed every 20 minutes by a background job.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved cached video"),
            @ApiResponse(responseCode = "404", description = "No cached video found for the given channel ID")
    })
    @GetMapping(value = "/youtube/{channelId}", produces = "application/json")
    public ResponseEntity<YoutubeVideoDto> getLatestYoutubeVideo(
            @Parameter(description = "YouTube channel ID", example = "UCbU41G2hhiwdn-gFFRqZN4w")
            @PathVariable String channelId) {
        if (!youTubeService.getTrackedChannelIds().contains(channelId)) {
            return ResponseEntity.notFound().build();
        }
        return youTubeService.getLatestVideo(channelId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get Discord widget data", description = "Returns cached Discord server presence data. Refreshed every 5 minutes by a background job.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved Discord widget data"),
            @ApiResponse(responseCode = "404", description = "No cached data for the given invite code")
    })
    @GetMapping(value = "/discord/{inviteCode}", produces = "application/json")
    public ResponseEntity<DiscordWidgetDataDto> getDiscordWidgetData(
            @Parameter(description = "Discord invite code", example = "uuc")
            @PathVariable String inviteCode) {
        if (!discordWidgetService.getTrackedInviteCodes().contains(inviteCode)) {
            return ResponseEntity.notFound().build();
        }
        return discordWidgetService.getWidgetData(inviteCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get all widget JSON schemas",
               description = "Returns a map of all known widget types to their respective JSON schemas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all widget schemas"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "403", description = "User lacks required cms:write permission")
    })
    @GetMapping(value = "/widget-schemas", produces = "application/json")
    @PreAuthorize("hasAuthority('cms:write')")
    public ResponseEntity<Map<String, JsonNode>> getAllWidgetSchemas() {
        return ResponseEntity.ok(widgetSchemaService.getAllSchemas());
    }

    @Operation(summary = "Get widget JSON schema",
               description = "Returns the JSON Schema for a specific widget type, reflecting its fields, validation constraints, and localization structure.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved widget schema"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated"),
            @ApiResponse(responseCode = "403", description = "User lacks required cms:write permission"),
            @ApiResponse(responseCode = "404", description = "Unknown widget type")
    })
    @GetMapping(value = "/widget-schemas/{type}", produces = "application/json")
    @PreAuthorize("hasAuthority('cms:write')")
    public ResponseEntity<JsonNode> getWidgetSchema(
            @Parameter(description = "Widget type identifier", example = "hero")
            @PathVariable String type) {
        return ResponseEntity.ok(widgetSchemaService.getSchema(type));
    }
}
