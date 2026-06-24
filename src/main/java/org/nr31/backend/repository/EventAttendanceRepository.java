package org.nr31.backend.repository;

import org.nr31.backend.model.EventAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EventAttendanceRepository extends JpaRepository<EventAttendance, Long> {
    List<EventAttendance> findByEventIdAndOccurrenceDate(Long eventId, Instant occurrenceDate);
    List<EventAttendance> findByMemberIdAndOccurrenceDateBetween(Long memberId, Instant from, Instant to);
    void deleteByEventIdAndOccurrenceDate(Long eventId, Instant occurrenceDate);
    long countByMemberIdAndOccurrenceDateBetween(Long memberId, Instant from, Instant to);
}
