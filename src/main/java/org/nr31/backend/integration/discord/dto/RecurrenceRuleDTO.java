package org.nr31.backend.integration.discord.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurrenceRuleDTO {

    @JsonProperty("by_month_day")
    private List<Integer> byMonthDay;

    @JsonProperty("by_weekday")
    private List<Integer> byWeekday;

    @JsonProperty("by_year_day")
    private List<Integer> byYearDay;

    private OffsetDateTime start;

    private Integer count;

    private Integer interval;

    private OffsetDateTime end;

    @JsonProperty("by_n_weekday")
    private List<Integer> byNWeekday;

    @JsonProperty("by_month")
    private List<Integer> byMonth;

    private Integer frequency;
}
