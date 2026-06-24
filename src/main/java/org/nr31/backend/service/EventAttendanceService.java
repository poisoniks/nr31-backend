package org.nr31.backend.service;

import org.nr31.backend.dto.attendance.EventAttendanceDTO;
import org.nr31.backend.dto.attendance.MemberMonthlyAttendanceDTO;

import java.time.Instant;
import java.util.List;

public interface EventAttendanceService {
    void recordAttendance(Long eventId, Instant occurrenceDate, List<Long> memberIds);
    List<EventAttendanceDTO> getAttendanceForOccurrence(Long eventId, Instant occurrenceDate);
    MemberMonthlyAttendanceDTO getMemberMonthlyAttendance(Long memberId, int year, int month);
    boolean isValidOccurrence(Long eventId, Instant occurrenceDate);
}
