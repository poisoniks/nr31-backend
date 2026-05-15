package org.nr31.backend.dto.cms;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nr31.backend.annotation.ImageField;
import org.nr31.backend.annotation.LocalizedField;
import org.nr31.backend.validation.ValidBackgroundImageId;
import org.nr31.backend.validation.ValidLocalizedString;

import java.util.Map;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Hero widget for full-viewport introduction section")
public class HeroWidgetDto extends WidgetDto {
    
    @NotNull(message = "Badge text must not be null")
    @ValidLocalizedString
    @LocalizedField
    @Schema(description = "Localized badge text above main title", 
            example = "{\"en\": \"M&B Bannerlord Regiment\", \"uk\": \"Полк M&B Bannerlord\"}", 
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> badgeText;
    
    @NotBlank(message = "Main title must not be blank")
    @Schema(description = "Primary brand text (not localized)", 
            example = "Nr.31", 
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String titleMain;
    
    @NotBlank(message = "Sub title must not be blank")
    @Schema(description = "Secondary brand text (not localized)", 
            example = "Feldkanonenregiment", 
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String titleSub;
    
    @NotNull(message = "Description must not be null")
    @ValidLocalizedString
    @LocalizedField
    @Schema(description = "Localized descriptive paragraph", 
            example = "{\"en\": \"Join the elite artillery regiment\", \"uk\": \"Приєднуйтесь до елітного артилерійського полку\"}", 
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> description;
    
    @NotNull(message = "CTA text must not be null")
    @ValidLocalizedString
    @LocalizedField
    @Schema(description = "Localized call-to-action button text", 
            example = "{\"en\": \"Join Now\", \"uk\": \"Приєднатися зараз\"}", 
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> ctaText;
    
    @NotBlank(message = "CTA target ID must not be blank")
    @Schema(description = "Anchor ID for scroll target", 
            example = "how-to-join", 
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String ctaTargetId;
    
    @NotNull(message = "Background image ID must not be null")
    @ValidBackgroundImageId
    @ImageField
    @Schema(description = "UUID of the background image from Library API", 
            example = "550e8400-e29b-41d4-a716-446655440000", 
            requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID backgroundImageId;
}
