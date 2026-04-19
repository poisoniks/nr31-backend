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
import org.nr31.backend.dto.MediaFolderDTO;
import org.nr31.backend.dto.MediaFolderRequest;
import org.nr31.backend.service.MediaFolderService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files/library/folders")
@RequiredArgsConstructor
@Tag(name = "Library Folders", description = "Endpoints for managing logical folder hierarchy in the media library")
@SecurityRequirement(name = "Bearer Authentication")
public class LibraryFolderController {

    private final MediaFolderService mediaFolderService;

    @Operation(summary = "List library folders",
            description = "Returns a list of logical folders in the media library. " +
                    "Pass parentId to list sub-folders; omit it to list root-level folders.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of folder metadata"),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('file:upload:public')")
    public ResponseEntity<List<MediaFolderDTO>> listFolders(@RequestParam(required = false) UUID parentId) {
        List<MediaFolderDTO> folders = mediaFolderService.listFolders(parentId);
        return ResponseEntity.ok(folders);
    }

    @Operation(summary = "Create a folder",
            description = "Creates a new logical directory in the media library. " +
                    "Specify parentId to nest inside an existing folder; omit it for a root-level folder.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Folder created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MediaFolderDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error (blank name, name too long)", content = @Content),
            @ApiResponse(responseCode = "404", description = "Parent folder not found", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('file:upload:public')")
    public ResponseEntity<MediaFolderDTO> createFolder(@Valid @RequestBody MediaFolderRequest request) {
        MediaFolderDTO dto = mediaFolderService.createFolder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @Operation(summary = "Rename or move a folder",
            description = "Updates the folder's name and/or parent. Pass a new parentId to move it; " +
                    "omit parentId (or set to null) to move it to root level.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Folder updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MediaFolderDTO.class))),
            @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
            @ApiResponse(responseCode = "404", description = "Folder or parent folder not found", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('file:upload:public')")
    public ResponseEntity<MediaFolderDTO> updateFolder(
            @PathVariable UUID id,
            @Valid @RequestBody MediaFolderRequest request) {
        MediaFolderDTO dto = mediaFolderService.updateFolder(id, request);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "Delete a folder",
            description = "Deletes a logical folder. Returns 409 Conflict if the folder contains " +
                    "files or sub-folders to prevent accidental mass-deletion of production assets.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Folder deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Folder not found", content = @Content),
            @ApiResponse(responseCode = "409", description = "Folder is not empty", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @DeleteMapping(value = "/{id}")
    @PreAuthorize("hasAuthority('file:delete')")
    public ResponseEntity<Void> deleteFolder(@PathVariable UUID id) {
        mediaFolderService.deleteFolder(id);
        return ResponseEntity.noContent().build();
    }
}
