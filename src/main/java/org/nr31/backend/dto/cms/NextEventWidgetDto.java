package org.nr31.backend.dto.cms;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nr31.backend.validation.ValidLocalizedString;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Next event widget with countdown timer")
public class NextEventWidgetDto extends WidgetDto {
    
    @ValidLocalizedString
    @Schema(description = "Optional localized title override", 
            example = "{\"en\": \"Upcoming Official Match\", \"uk\": \"Наступний офіційний матч\"}", 
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Map<String, String> titleOverride;
}
