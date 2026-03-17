package org.nr31.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "event_exceptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEventException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_event_id", nullable = false)
    private CalendarEvent originalEvent;

    @Column(name = "exception_date", nullable = false)
    private Instant exceptionDate;

    @Column(name = "is_cancelled", nullable = false)
    private boolean isCancelled;

    @Column(name = "new_title")
    private Map<String, String> newTitle;

    @Column(name = "new_description")
    private Map<String, String> newDescription;

    @Column(name = "new_start_time")
    private Instant newStart;

    @Column(name = "new_end_time")
    private Instant newEnd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_type_id")
    private EventType newType;

    @Column(name = "new_server_name")
    private String newServerName;

    @Column(name = "discord_exception_id", unique = true)
    private String discordExceptionId;
}
