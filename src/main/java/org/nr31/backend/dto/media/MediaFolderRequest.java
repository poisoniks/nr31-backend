package org.nr31.backend.dto.media;

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
@Schema(description = "Request payload for creating or updating a media folder")
public class MediaFolderRequest {

    @NotBlank(message = "Folder name must not be blank")
    @Size(max = 255, message = "Folder name must not exceed 255 characters")
    @Schema(description = "Folder name", example = "Banners", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "UUID of the parent folder; omit for a root-level folder", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID parentId;
}
