package org.nr31.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request representation for creating a new calendar event")
public class CreateEventRequest {

    @Schema(description = "Localized event title", example = "{\"en\": \"Training\", \"uk\": \"Тренування\"}")
    private Map<String, String> title;

    @Schema(description = "Localized event description", example = "{\"en\": \"Weekly training\", \"uk\": \"Щотижневе тренування\"}")
    private Map<String, String> description;

    @Schema(description = "Start time of the event in ISO-8601 format", example = "2026-10-27T10:00:00Z")
    private String start;

    @Schema(description = "End time of the event in ISO-8601 format", example = "2026-10-27T12:00:00Z")
    private String end;

    @Schema(description = "Identifier of the event type", example = "1")
    private Long type;

    @Schema(description = "Name of the server where the event takes place", example = "Main Server")
    private String serverName;

    @Schema(description = "List of unit identifiers participating in the event")
    private List<Long> participatingUnits;

    @Schema(description = "Recurrence rules for the event")
    private Recurrence recurrence;

    @Schema(description = "Timezone of the event", example = "Europe/Kyiv")
    private String timezone;
}
