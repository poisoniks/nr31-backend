package org.nr31.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Payload for patching a library file record (rename and/or move)")
public class LibraryFileUpdateRequest {

    @NotBlank(message = "File name cannot be empty")
    @Size(max = 255, message = "File name must not exceed 255 characters")
    @Schema(description = "New user-defined name for the file (renames original_name); omit to leave unchanged", example = "new-banner.png")
    private String name;

    @Schema(description = "UUID of the target folder; send null to move the file to root level; omit the field to leave unchanged")
    private UUID folderId;
}
