package org.nr31.backend.dto.cms;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Image widget")
public class ImageWidgetDto extends WidgetDto {
    
    @NotBlank(message = "Image URL must not be blank")
    @Schema(description = "URL of the image", example = "https://example.com/image.jpg", requiredMode = Schema.RequiredMode.REQUIRED)
    private String url;
    
    @Schema(description = "Alt text for accessibility", example = "Regiment banner", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String alt;
}
