package org.nr31.backend.dto.cms;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;
import org.nr31.backend.dto.ErrorResponse;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@Schema(description = "Response containing CMS layout validation error details")
public class CmsValidationErrorResponse extends ErrorResponse {
    @Schema(description = "Map of field/widget identifiers to translation keys")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> details;

    @Schema(description = "Map of field/widget identifiers to their translation interpolation context parameters")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, Map<String, Object>> context;
}
