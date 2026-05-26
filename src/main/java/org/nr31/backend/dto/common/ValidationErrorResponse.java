package org.nr31.backend.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@Schema(description = "Response containing validation error details")
public class ValidationErrorResponse extends ErrorResponse {
    @Schema(description = "Map of field names to error messages")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> details;
}
