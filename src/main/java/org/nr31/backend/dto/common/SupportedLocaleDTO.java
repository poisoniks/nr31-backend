package org.nr31.backend.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO representing a supported locale")
public class SupportedLocaleDTO {
    @Schema(description = "Unique identifier of the locale", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "ISO 639-1 language code", example = "en", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Schema(description = "Human-readable description of the locale", example = "English", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;
}
