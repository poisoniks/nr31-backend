package org.nr31.backend.repository;

import org.nr31.backend.model.CalendarEvent;
import org.nr31.backend.model.CalendarEventException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarEventExceptionRepository extends JpaRepository<CalendarEventException, Long> {

        @Query("SELECT ex FROM CalendarEventException ex " +
                        "LEFT JOIN FETCH ex.newType " +
                        "WHERE ex.originalEvent.id IN :eventIds " +
                        "AND ex.exceptionDate >= :windowStart " +
                        "AND ex.exceptionDate <= :windowEnd")
        List<CalendarEventException> findExceptionsForEventsInWindow(
                        @Param("eventIds") List<Long> eventIds,
                        @Param("windowStart") Instant windowStart,
                        @Param("windowEnd") Instant windowEnd);

        Optional<CalendarEventException> findByOriginalEventAndExceptionDate(CalendarEvent originalEvent,
                        Instant exceptionDate);

        List<CalendarEventException> findByOriginalEvent(CalendarEvent originalEvent);

        void deleteByOriginalEvent(CalendarEvent originalEvent);

    Optional<CalendarEventException> findByDiscordExceptionId(String discordExceptionId);
}
