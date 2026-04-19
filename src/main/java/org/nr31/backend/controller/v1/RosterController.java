package org.nr31.backend.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.ErrorResponse;
import org.nr31.backend.dto.EventTypeDTO;
import org.nr31.backend.dto.EventTypeRequest;
import org.nr31.backend.dto.UnitTypeDTO;
import org.nr31.backend.dto.UnitTypeRequest;
import org.nr31.backend.service.RosterService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/roster")
@RequiredArgsConstructor
@Tag(name = "Roster", description = "Endpoints for roster management: event types and unit types")
public class RosterController {

        private final RosterService rosterService;

        @Operation(summary = "Get all unit types", description = "Retrieves all available unit types")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved unit types")
        })
        @GetMapping(value = "/unit-types", produces = "application/json")
        public ResponseEntity<Page<UnitTypeDTO>> getAllUnitTypes(@PageableDefault(size = 20) Pageable pageable) {
                return ResponseEntity.ok(rosterService.getAllUnitTypes(pageable));
        }

        @Operation(summary = "Get unit type by ID", description = "Retrieves a specific unit type by ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved unit type", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UnitTypeDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Unit type not found",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        })
        @GetMapping(value = "/unit-types/{id}", produces = "application/json")
        public ResponseEntity<UnitTypeDTO> getUnitTypeById(@PathVariable Long id) {
                return rosterService.getUnitTypeById(id)
                                .map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        }

        @Operation(summary = "Create unit type", description = "Creates a new unit type", security = @SecurityRequirement(name = "Bearer Authentication"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Successfully created unit type", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UnitTypeDTO.class)))
        })
        @PostMapping(value = "/unit-types", produces = "application/json", consumes = "application/json")
        @PreAuthorize("hasAuthority('roster:write')")
        public ResponseEntity<UnitTypeDTO> createUnitType(@Valid @RequestBody UnitTypeRequest request) {
                UnitTypeDTO created = rosterService.createUnitType(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(created);
        }

        @Operation(summary = "Update unit type", description = "Updates an existing unit type", security = @SecurityRequirement(name = "Bearer Authentication"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully updated unit type", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UnitTypeDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Unit type not found",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        })
        @PutMapping(value = "/unit-types/{id}", produces = "application/json", consumes = "application/json")
        @PreAuthorize("hasAuthority('roster:write')")
        public ResponseEntity<UnitTypeDTO> updateUnitType(@PathVariable Long id,
                        @Valid @RequestBody UnitTypeRequest request) {
                UnitTypeDTO updated = rosterService.updateUnitType(id, request);
                return ResponseEntity.ok(updated);
        }

        @Operation(summary = "Delete unit type", description = "Deletes a unit type by ID", security = @SecurityRequirement(name = "Bearer Authentication"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Successfully deleted unit type"),
                        @ApiResponse(responseCode = "404", description = "Unit type not found",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        })
        @DeleteMapping(value = "/unit-types/{id}", produces = "application/json")
        @PreAuthorize("hasAuthority('roster:write')")
        public ResponseEntity<Void> deleteUnitType(@PathVariable Long id) {
                rosterService.deleteUnitType(id);
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Get all event types", description = "Retrieves all available event types")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved event types")
        })
        @GetMapping(value = "/event-types", produces = "application/json")
        public ResponseEntity<Page<EventTypeDTO>> getAllEventTypes(@PageableDefault(size = 20) Pageable pageable) {
                return ResponseEntity.ok(rosterService.getAllEventTypes(pageable));
        }

        @Operation(summary = "Get event type by ID", description = "Retrieves a specific event type by ID")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved event type", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventTypeDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Event type not found",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        })
        @GetMapping(value = "/event-types/{id}", produces = "application/json")
        public ResponseEntity<EventTypeDTO> getEventTypeById(@PathVariable Long id) {
                return rosterService.getEventTypeById(id)
                                .map(ResponseEntity::ok)
                                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        }

        @Operation(summary = "Create event type", description = "Creates a new event type", security = @SecurityRequirement(name = "Bearer Authentication"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Successfully created event type", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventTypeDTO.class)))
        })
        @PostMapping(value = "/event-types", produces = "application/json", consumes = "application/json")
        @PreAuthorize("hasAuthority('roster:write')")
        public ResponseEntity<EventTypeDTO> createEventType(@Valid @RequestBody EventTypeRequest request) {
                EventTypeDTO created = rosterService.createEventType(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(created);
        }

        @Operation(summary = "Update event type", description = "Updates an existing event type", security = @SecurityRequirement(name = "Bearer Authentication"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully updated event type", content = @Content(mediaType = "application/json", schema = @Schema(implementation = EventTypeDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Event type not found",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        })
        @PutMapping(value = "/event-types/{id}", produces = "application/json", consumes = "application/json")
        @PreAuthorize("hasAuthority('roster:write')")
        public ResponseEntity<EventTypeDTO> updateEventType(@PathVariable Long id,
                        @Valid @RequestBody EventTypeRequest request) {
                EventTypeDTO updated = rosterService.updateEventType(id, request);
                return ResponseEntity.ok(updated);
        }

        @Operation(summary = "Delete event type", description = "Deletes an event type by ID", security = @SecurityRequirement(name = "Bearer Authentication"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "Successfully deleted event type"),
                        @ApiResponse(responseCode = "404", description = "Event type not found",
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        })
        @DeleteMapping(value = "/event-types/{id}", produces = "application/json")
        @PreAuthorize("hasAuthority('roster:write')")
        public ResponseEntity<Void> deleteEventType(@PathVariable Long id) {
                rosterService.deleteEventType(id);
                return ResponseEntity.noContent().build();
        }
}
