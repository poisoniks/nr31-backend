package org.nr31.backend.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.common.ErrorResponse;
import org.nr31.backend.dto.media.FileMetadataDTO;
import org.nr31.backend.dto.media.FileUploadResponse;
import org.nr31.backend.dto.media.LibraryFileUpdateRequest;
import org.nr31.backend.dto.common.ValidationErrorResponse;
import org.nr31.backend.service.FileStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files/library/files")
@RequiredArgsConstructor
@Tag(name = "Library Files", description = "Endpoints for managing files in the media library")
@SecurityRequirement(name = "Bearer Authentication")
public class LibraryFileController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "List library files",
            description = "Returns a paginated list of LIBRARY files. " +
                    "Pass ?folderId to list files in a specific folder; omit it to list root-level files.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page of file metadata"),
            @ApiResponse(responseCode = "404", description = "Folder not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('file:upload:public')")
    public ResponseEntity<Page<FileMetadataDTO>> listFiles(
            @RequestParam(required = false) UUID folderId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<FileMetadataDTO> page = fileStorageService.listLibraryFiles(folderId, pageable);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Upload a library file",
            description = "Uploads a file as a LIBRARY asset (immune to garbage collection). " +
                    "Pass folderId to place the file in a folder; omit it for root-level. " +
                    "Allowed types: image/png, image/jpeg, image/webp. Max size: 5MB.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "File uploaded successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileUploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file (empty, wrong type, or too large)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Folder not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('file:upload:public')")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderId", required = false) UUID folderId,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        FileUploadResponse response = fileStorageService.storeLibraryFile(file, principal.getName(), folderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Rename or move a library file",
            description = "Updates the file's user-defined name (original_name) and/or moves it to a different folder. " +
                    "The physical file on disk is completely untouched.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File metadata updated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = FileMetadataDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "File or target folder not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('file:upload:public')")
    public ResponseEntity<FileMetadataDTO> updateFile(
            @PathVariable UUID id,
            @Valid @RequestBody LibraryFileUpdateRequest request) {
        FileMetadataDTO dto = fileStorageService.updateLibraryFile(id, request);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Delete a library file",
            description = "Drops the FileMetadata record (logical delete). " +
                    "The physical file on disk is left intact and will be cleaned up by the " +
                    "scheduled FileCleanupJob if no other metadata records reference the same hash.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "File deleted successfully"),
            @ApiResponse(responseCode = "404", description = "File not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping(value = "/{id}")
    @PreAuthorize("hasAuthority('file:delete')")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID id) {
        fileStorageService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }
}
