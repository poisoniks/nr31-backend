package org.nr31.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nr31.backend.model.EventType;
import org.nr31.backend.model.UnitType;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalendarEventDTO {
    private String id;
    private String seriesId;
    private Map<String, String> title;
    private Map<String, String> description;
    private String start;
    private String end;
    private EventType type;
    private String customIcon;
    private String serverName;
    private List<UnitType> participatingUnits;
    private boolean isRecurring;
}
