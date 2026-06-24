package org.nr31.backend.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAttendanceDTO {
    private Long memberId;
    private String memberNickname;
    private Long eventId;
    private Map<String, String> eventTitle;
    private Instant occurrenceDate;
    private Instant createdAt;
}
