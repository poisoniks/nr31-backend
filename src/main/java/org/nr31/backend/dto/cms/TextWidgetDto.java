package org.nr31.backend.dto.cms;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Text content widget")
public class TextWidgetDto extends WidgetDto {
    
    @NotBlank(message = "Text content must not be blank")
    @Schema(description = "HTML content of the text widget", example = "<p>Welcome to our site</p>", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
}
