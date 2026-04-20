package org.nr31.backend.dto.cms;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Video widget")
public class VideoWidgetDto extends WidgetDto {
    
    @NotBlank(message = "Video URL must not be blank")
    @Schema(description = "URL of the video", example = "https://example.com/video.mp4", requiredMode = Schema.RequiredMode.REQUIRED)
    private String url;
}
