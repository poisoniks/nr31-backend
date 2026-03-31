package org.nr31.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response containing validation error details")
public class ValidationErrorResponse {
    @Schema(description = "Map of field names to error messages")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> details;

    @Schema(description = "Error message", example = "Validation failed", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "Timestamp when the error occurred", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime timestamp;
}
