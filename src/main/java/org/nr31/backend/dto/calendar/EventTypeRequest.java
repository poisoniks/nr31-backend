package org.nr31.backend.dto.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
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

    @Schema(description = "UUID of the custom icon file to associate with this event type", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID customIcon;

    @Schema(description = "Weight of the event type for attendance calculation", example = "1")
    private Integer attendanceWeight;

}
