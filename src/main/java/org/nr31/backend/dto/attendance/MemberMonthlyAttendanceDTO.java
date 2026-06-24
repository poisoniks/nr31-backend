package org.nr31.backend.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nr31.backend.model.AttendanceStatus;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberMonthlyAttendanceDTO {
    private Long memberId;
    private int year;
    private int month;
    private int eventAttendanceScore;
    private int manualAttendanceCount;
    private int totalScore;
    private int maxPossibleScore;
    private AttendanceStatus status;
    private List<EventAttendanceDTO> attendedEvents;
}
