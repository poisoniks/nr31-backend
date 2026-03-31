package org.nr31.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response containing uploaded file metadata")
public class FileUploadResponse {
    @Schema(description = "Unique identifier of the uploaded file", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;

    @Schema(description = "Original filename", example = "training_plan.pdf", requiredMode = Schema.RequiredMode.REQUIRED)
    private String originalName;

    @Schema(description = "Temporary download URL for the file", example = "upload/550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
    private String url;

    @Schema(description = "File size in bytes", example = "1048576", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long size;
}
