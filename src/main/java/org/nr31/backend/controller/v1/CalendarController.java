package org.nr31.backend.controller.v1;

import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.CalendarActionMode;
import org.nr31.backend.dto.CalendarEventDTO;
import org.nr31.backend.dto.CreateEventRequest;
import org.nr31.backend.dto.UpdateEventRequest;
import org.nr31.backend.service.CalendarService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/calendar/events")
@RequiredArgsConstructor
@Tag(name = "Calendar", description = "Endpoints for calendar events management")
public class CalendarController {

    private final CalendarService calendarService;

    @Operation(summary = "Get calendar events", description = "Retrieves a list of calendar events for a specific date range")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved events", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = CalendarEventDTO.class)))),
            @ApiResponse(responseCode = "400", description = "Invalid date range parameters", content = @Content)
    })
    @GetMapping(produces = "application/json")
    public ResponseEntity<List<CalendarEventDTO>> getEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Instant fromInstant = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toInstant = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<CalendarEventDTO> events = calendarService.getEvents(fromInstant, toInstant);
        return ResponseEntity.ok(events);
    }

    @Operation(summary = "Create calendar event", description = "Creates a new calendar event")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully created event", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CalendarEventDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    })
    @PostMapping(produces = "application/json", consumes = "application/json")
    @PreAuthorize("hasAuthority('event:write')")
    public ResponseEntity<CalendarEventDTO> createEvent(@RequestBody CreateEventRequest request) {
        CalendarEventDTO created = calendarService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update calendar event", description = "Updates an existing calendar event by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated event", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CalendarEventDTO.class))),
            @ApiResponse(responseCode = "404", description = "Event not found", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid request body or path parameter", content = @Content)
    })
    @PutMapping(value = "/{id}", produces = "application/json", consumes = "application/json")
    @PreAuthorize("hasAuthority('event:write')")
    public ResponseEntity<CalendarEventDTO> updateEvent(
            @PathVariable Long id,
            @RequestBody UpdateEventRequest request) {
        CalendarEventDTO updated = calendarService.updateEvent(id, request);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Delete calendar event", description = "Deletes a calendar event by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully deleted event"),
            @ApiResponse(responseCode = "404", description = "Event not found", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content)
    })
    @DeleteMapping(value = "/{id}", produces = "application/json")
    @PreAuthorize("hasAuthority('event:write')")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long id,
            @RequestParam CalendarActionMode mode,
            @RequestParam(required = false) Instant exceptionDate) {
        calendarService.deleteEvent(id, mode, exceptionDate);
        return ResponseEntity.noContent().build();
    }
}
