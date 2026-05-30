package org.nr31.backend.dto.media;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Representation of a media library folder")
public class MediaFolderDTO {

    @Schema(description = "Unique folder identifier", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;

    @Schema(description = "Folder name", example = "Banners", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Parent folder UUID; null for root-level folders", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID parentId;

    @Schema(description = "Creation timestamp", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant createdAt;
}
