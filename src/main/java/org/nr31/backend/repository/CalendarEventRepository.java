package org.nr31.backend.repository;

import org.nr31.backend.model.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    @Query("SELECT e FROM CalendarEvent e " +
            "LEFT JOIN FETCH e.type " +
            "LEFT JOIN FETCH e.participatingUnits " +
            "WHERE " +
            "(e.rrule IS NULL AND e.start < :windowEnd AND e.end > :windowStart) OR " +
            "(e.rrule IS NOT NULL AND e.start < :windowEnd)")
    List<CalendarEvent> findEventsInWindowOrRecurring(
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd);

    List<CalendarEvent> findBySeriesId(String seriesId);
}
