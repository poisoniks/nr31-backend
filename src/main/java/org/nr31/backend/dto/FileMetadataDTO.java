package org.nr31.backend.dto;

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
@Schema(description = "Rich metadata representation of a file in the media library")
public class FileMetadataDTO {

    @Schema(description = "Unique file identifier", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;

    @Schema(description = "User-defined file name (original_name column)", example = "banner-spring.png", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Public access URL", example = "/api/v1/files/550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
    private String url;

    @Schema(description = "MIME content type", example = "image/png", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contentType;

    @Schema(description = "File size in bytes", example = "204800", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sizeBytes;

    @Schema(description = "UUID of the containing folder; null if file lives at root level")
    private UUID folderId;

    @Schema(description = "Username of the uploader", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String uploaderUsername;

    @Schema(description = "Upload timestamp", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant createdAt;
}
