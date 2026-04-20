package org.nr31.backend.dto.cms;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Embed widget for external content")
public class EmbedWidgetDto extends WidgetDto {
    
    @NotBlank(message = "Embed code must not be blank")
    @Schema(description = "HTML embed code", example = "<iframe src='...'></iframe>", requiredMode = Schema.RequiredMode.REQUIRED)
    private String embedCode;
}
