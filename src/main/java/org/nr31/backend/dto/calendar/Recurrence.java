package org.nr31.backend.dto.calendar;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Recurrence rules for a calendar event")
public class Recurrence {

    @Schema(description = "Frequency of recurrence", example = "WEEKLY")
    private String frequency;

    @Schema(description = "Interval of recurrence", example = "1")
    private Integer interval;

    @Schema(description = "Number of occurrences", example = "10")
    private Integer count;

    @Schema(description = "End date of recurrence in ISO-8601 format", example = "2026-12-31T23:59:59Z")
    private String until;

    @Schema(description = "Days of the week for recurrence", example = "[\"MO\", \"WE\", \"FR\"]")
    private List<String> byDay;
}