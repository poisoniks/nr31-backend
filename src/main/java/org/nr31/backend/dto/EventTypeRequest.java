package org.nr31.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nr31.backend.validation.ValidLocalizedString;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request representation for an event type")
public class EventTypeRequest {

    @Schema(description = "Localized event type name", example = "{\"en\": \"Training\", \"uk\": \"Тренування\"}")
    @ValidLocalizedString
    private Map<String, String> name;

    @Schema(description = "Custom icon associated with this event type", example = "/aim_icon")
    private String customIcon;

}
