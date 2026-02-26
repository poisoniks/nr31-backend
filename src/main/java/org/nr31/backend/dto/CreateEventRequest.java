package org.nr31.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateEventRequest {
    private Map<String, String> title;
    private Map<String, String> description;
    private String start;
    private String end;
    private Long type;
    private String serverName;
    private List<Long> participatingUnits;
    private Recurrence recurrence;
    private String timezone;
}
