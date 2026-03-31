package org.nr31.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO representing a type of calendar event")
public class EventTypeDTO {

    @Schema(description = "Unique identifier of the event type", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "Localized name of the event type", example = "{\"en\": \"Training\", \"uk\": \"Тренування\"}", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> name;

    @Schema(description = "Custom icon associated with this event type", example = "target")
    private String customIcon;
}
