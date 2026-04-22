package org.nr31.backend.dto.cms;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nr31.backend.validation.ValidLocalizedString;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Embed widget for external content")
public class EmbedWidgetDto extends WidgetDto {
    
    @NotNull(message = "Embed code must not be null")
    @ValidLocalizedString
    @Schema(description = "Localized HTML embed code", example = "{\"en\": \"<iframe src='...'></iframe>\", \"uk\": \"<iframe src='...'></iframe>\"}", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> embedCode;
}
