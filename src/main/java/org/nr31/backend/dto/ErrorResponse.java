package org.nr31.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Generic error response")
public class ErrorResponse {
    @Schema(description = "Error message", example = "An unexpected error occurred", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "Standardized error code", example = "INTERNAL_SERVER_ERROR", requiredMode = Schema.RequiredMode.REQUIRED)
    private ErrorCode code;

    @Schema(description = "Timestamp when the error occurred", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime timestamp;

    @Schema(description = "Additional metadata related to the error")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> metadata;
}
