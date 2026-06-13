package org.nr31.backend.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.common.ErrorResponse;
import org.nr31.backend.dto.calendar.EventTypeDTO;
import org.nr31.backend.dto.calendar.EventTypeRequest;
import org.nr31.backend.dto.common.ValidationErrorResponse;
import org.nr31.backend.service.EventTypeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/calendar/event-types")
@RequiredArgsConstructor
@Tag(name = "Event Types", description = "Endpoints for event type management")
public class EventTypeController {

    private final EventTypeService eventTypeService;

    @Operation(summary = "Get all event types", description = "Retrieves all available event types")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved event types")
    })
    @GetMapping(produces = "application/json")
    public ResponseEntity<Page<EventTypeDTO>> getAllEventTypes(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(eventTypeService.getAllEventTypes(pageable));
    }

    @Operation(summary = "Get event type by ID", description = "Retrieves a specific event type by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved event type", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventTypeDTO.class))),
            @ApiResponse(responseCode = "404", description = "Event type not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(value = "/{id}", produces = "application/json")
    public ResponseEntity<EventTypeDTO> getEventTypeById(@PathVariable Long id) {
        return eventTypeService.getEventTypeById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Operation(summary = "Create event type", description = "Creates a new event type", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully created event type", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventTypeDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @PostMapping(produces = "application/json", consumes = "application/json")
    @PreAuthorize("hasAuthority('event:write')")
    public ResponseEntity<EventTypeDTO> createEventType(@Valid @RequestBody EventTypeRequest request) {
        EventTypeDTO created = eventTypeService.createEventType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update event type", description = "Updates an existing event type", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated event type", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventTypeDTO.class))),
            @ApiResponse(responseCode = "404", description = "Event type not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @PutMapping(value = "/{id}", produces = "application/json", consumes = "application/json")
    @PreAuthorize("hasAuthority('event:write')")
    public ResponseEntity<EventTypeDTO> updateEventType(@PathVariable Long id,
                                                         @Valid @RequestBody EventTypeRequest request) {
        EventTypeDTO updated = eventTypeService.updateEventType(id, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete event type", description = "Deletes an event type by ID", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted event type"),
            @ApiResponse(responseCode = "404", description = "Event type not found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping(value = "/{id}", produces = "application/json")
    @PreAuthorize("hasAuthority('event:write')")
    public ResponseEntity<Void> deleteEventType(@PathVariable Long id) {
        eventTypeService.deleteEventType(id);
        return ResponseEntity.noContent().build();
    }
}
