package org.nr31.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Generic error response")
public class ErrorResponse {
    @Schema(description = "Error message", example = "An unexpected error occurred", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "Timestamp when the error occurred", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime timestamp;
}
