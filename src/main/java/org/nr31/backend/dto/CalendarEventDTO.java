package org.nr31.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nr31.backend.integration.discord.EventSource;
import org.nr31.backend.model.EventType;
import org.nr31.backend.model.UnitType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Data transfer object representing a calendar event")
public class CalendarEventDTO {

    @Schema(description = "Unique identifier of the event", example = "1")
    private String id;

    @Schema(description = "Identifier for the series of recurring events", example = "series-123")
    private String seriesId;

    @Schema(description = "Localized event title", example = "{\"en\": \"Training\", \"uk\": \"Тренування\"}")
    private Map<String, String> title;

    @Schema(description = "Localized event description", example = "{\"en\": \"Weekly training\", \"uk\": \"Щотижневе тренування\"}")
    private Map<String, String> description;

    @Schema(description = "Start time of the event in ISO-8601 format", example = "2026-10-27T10:00:00Z")
    private OffsetDateTime start;

    @Schema(description = "End time of the event in ISO-8601 format", example = "2026-10-27T12:00:00Z")
    private OffsetDateTime end;

    @Schema(description = "Type of the event")
    private EventType type;

    @Schema(description = "Name of the server where the event takes place", example = "Main Server")
    private String serverName;

    @Schema(description = "List of units participating in the event")
    private List<UnitType> participatingUnits;

    @Schema(description = "Indicates whether the event is recurring", example = "false")
    private boolean isRecurring;

    @Schema(description = "Source of the event (SITE or DISCORD)", example = "SITE")
    private EventSource source;

    @Schema(description = "Discord Scheduled Event ID if the event is synced from Discord")
    private String discordId;

    @Schema(description = "Indicates whether the event occurrence is cancelled", example = "false")
    private boolean isCancelled;
}
