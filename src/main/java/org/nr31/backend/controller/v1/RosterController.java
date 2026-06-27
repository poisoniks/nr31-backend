package org.nr31.backend.controller.v1;
import java.security.Principal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.attendance.MemberMonthlyAttendanceDTO;
import org.nr31.backend.dto.common.ErrorResponse;
import org.nr31.backend.dto.roster.UnitTypeDTO;
import org.nr31.backend.dto.roster.UnitTypeRequest;
import org.nr31.backend.dto.common.ValidationErrorResponse;
import org.nr31.backend.service.EventAttendanceService;
import org.nr31.backend.service.RosterService;
import org.nr31.backend.service.RosterImportExportService;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
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
        private final RosterImportExportService rosterImportExportService;
        private final EventAttendanceService eventAttendanceService;

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
                        @ApiResponse(responseCode = "201", description = "Successfully created unit type", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UnitTypeDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationErrorResponse.class)))
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
                                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationErrorResponse.class)))
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

        @Operation(summary = "Import roster from Excel", description = "Uploads a roster Excel file and parses it to populate members, attendance, and training data.", security = @SecurityRequirement(name = "Bearer Authentication"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully imported roster"),
                        @ApiResponse(responseCode = "400", description = "Invalid file or parsing error", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        })
        @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("hasAuthority('roster:write')")
        public ResponseEntity<Void> importRoster(@RequestParam("file") MultipartFile file, Principal principal) {
                rosterImportExportService.importFromExcel(file, principal.getName());
                return ResponseEntity.ok().build();
        }

        @Operation(summary = "Export roster to Excel", description = "Generates a roster Excel file in the configured template format containing current registry, attendance, and training data.", security = @SecurityRequirement(name = "Bearer Authentication"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully generated roster Excel"),
                        @ApiResponse(responseCode = "404", description = "Template file not found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        })
        @GetMapping(value = "/export")
        @PreAuthorize("hasAuthority('roster:read')")
        public ResponseEntity<StreamingResponseBody> exportRoster() {
                return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"roster.xlsx\"")
                                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                                .body(rosterImportExportService::exportToExcel);
        }

        @Operation(summary = "Upload roster export template", description = "Uploads a new roster Excel template to be used for exporting.", security = @SecurityRequirement(name = "Bearer Authentication"))
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully uploaded template"),
                        @ApiResponse(responseCode = "400", description = "Invalid template file", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
                        @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        })
        @PostMapping(value = "/template", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @PreAuthorize("hasAuthority('roster:write')")
        public ResponseEntity<Void> uploadTemplate(@RequestParam("file") MultipartFile file, Principal principal) {
                rosterImportExportService.uploadTemplate(file, principal.getName());
                return ResponseEntity.ok().build();
        }

        @Operation(summary = "Get monthly attendance for member", description = "Retrieves the monthly attendance summary for a roster member, including per-event breakdown", security = @SecurityRequirement(name = "Bearer Authentication"))
        @ApiResponses(value = {
                @ApiResponse(responseCode = "200", description = "Successfully retrieved attendance", content = @Content(mediaType = "application/json", schema = @Schema(implementation = org.nr31.backend.dto.attendance.MemberMonthlyAttendanceDTO.class))),
                @ApiResponse(responseCode = "404", description = "Member not found",
                        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
        })
        @GetMapping(value = "/members/{memberId}/attendance", produces = "application/json")
        @PreAuthorize("hasAuthority('roster:read')")
        public ResponseEntity<MemberMonthlyAttendanceDTO> getMemberMonthlyAttendance(
                @PathVariable Long memberId,
                @RequestParam int year,
                @RequestParam int month) {
                MemberMonthlyAttendanceDTO attendance = eventAttendanceService.getMemberMonthlyAttendance(memberId, year, month);
                return ResponseEntity.ok(attendance);
        }
}
