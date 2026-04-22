package org.nr31.backend.dto.cms;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nr31.backend.validation.ValidLocalizedString;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Text content widget")
public class TextWidgetDto extends WidgetDto {
    
    @NotNull(message = "Content must not be null")
    @ValidLocalizedString
    @Schema(description = "Localized HTML content of the text widget", example = "{\"en\": \"<p>Welcome to our site</p>\", \"uk\": \"<p>Ласкаво просимо на наш сайт</p>\"}", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> content;
}
