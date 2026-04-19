package org.nr31.backend.controller.v1;

import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.FileUploadResponse;
import org.nr31.backend.model.FileMetadata;
import org.nr31.backend.model.FileScope;
import org.nr31.backend.model.Role;
import org.nr31.backend.repository.RoleRepository;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.service.FileStorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "Endpoints for file storage management")
@SecurityRequirement(name = "Bearer Authentication")
public class FileController {

    private final FileStorageService fileStorageService;
    private final RoleRepository roleRepository;

    @Operation(summary = "Upload an attachment file",
            description = "Uploads a file as an ATTACHMENT (subject to garbage collection if not linked within 24h). " +
                    "Allowed types: image/png, image/jpeg, image/webp. Max size: 5MB.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "File uploaded successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FileUploadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid file (empty, wrong type, or too large)", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @PostMapping(value = "/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('file:upload:attachment')")
    public ResponseEntity<FileUploadResponse> uploadAttachment(
            @RequestParam("file") MultipartFile file,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        FileUploadResponse response = fileStorageService.storeFile(file, principal.getName(), FileScope.ATTACHMENT);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @Operation(summary = "Get a file", description = "Resolves a file by UUID and returns an X-Accel-Redirect response for nginx to serve the physical file")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File resolved, X-Accel-Redirect header set"),
            @ApiResponse(responseCode = "404", description = "File not found", content = @Content)
    })
    @GetMapping(value = "/{id}")
    public ResponseEntity<Void> getFile(@PathVariable UUID id) {
        FileMetadata metadata = fileStorageService.resolveFile(id);

        return ResponseEntity.ok()
                .header("X-Accel-Redirect", "/internal-files/" + metadata.getStoredName())
                .header(HttpHeaders.CONTENT_TYPE, metadata.getContentType())
                .build();
    }

    @Operation(summary = "Delete a file", description = "Deletes a file's metadata by UUID. Physical file cleanup is handled by the scheduled garbage collector.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "File deleted successfully"),
            @ApiResponse(responseCode = "404", description = "File not found", content = @Content),
            @ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content)
    })
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAuthority('file:delete')")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID id) {
        fileStorageService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update file upload quota for a role", description = "Updates the filesUploadQuotaBytes for a given role ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Quota updated successfully"),
            @ApiResponse(responseCode = "404", description = "Role not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PatchMapping("/quota/role/{id}")
    @PreAuthorize("hasAuthority('file:manage_quota')")
    public ResponseEntity<Void> updateRoleQuota(@PathVariable Long id, @RequestParam Long quotaBytes) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ElementNotFoundException("Role not found"));
        role.setFilesUploadQuotaBytes(quotaBytes);
        roleRepository.save(role);
        return ResponseEntity.noContent().build();
    }
}
