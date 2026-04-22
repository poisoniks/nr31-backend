package org.nr31.backend.dto.cms;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nr31.backend.validation.ValidLocalizedString;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Image widget")
public class ImageWidgetDto extends WidgetDto {
    
    @NotBlank(message = "Image URL must not be blank")
    @Schema(description = "URL of the image", example = "https://example.com/image.jpg", requiredMode = Schema.RequiredMode.REQUIRED)
    private String url;
    
    @ValidLocalizedString
    @Schema(description = "Localized alt text for accessibility", example = "{\"en\": \"Regiment banner\", \"uk\": \"Банер полку\"}", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Map<String, String> alt;
}
