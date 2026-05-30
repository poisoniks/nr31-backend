package org.nr31.backend.dto.cms;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "YouTube widget that displays the latest video from a single channel")
public class YoutubeWidgetDto extends WidgetDto {

    @NotBlank(message = "Channel ID must not be blank")
    @Pattern(regexp = "^UC[\\w-]{22}$", message = "Channel ID must be a valid YouTube channel ID starting with 'UC'")
    @Schema(description = "YouTube channel ID to track (e.g. UCbU41G2hhiwdn-gFFRqZN4w)",
            example = "UCbU41G2hhiwdn-gFFRqZN4w",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String channelId;
}
