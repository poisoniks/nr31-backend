package org.nr31.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for application configuration")
public class AppConfigDto {

    @Schema(description = "Name of the property")
    private String name;

    @Schema(description = "Description of the configuration (localized)", example = "{\"en\": \"description\", \"uk\": \"опис\"}")
    private Map<String, String> description;

    @NotNull(message = "Config value is required")
    @Schema(description = "The actual configuration value as a JSON object, validated by schema")
    private String configValue;

    @Schema(description = "JSON schema used to validate the configValue")
    private String configSchema;
}
