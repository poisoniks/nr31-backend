package org.nr31.backend.controller.v1;

import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.CalendarActionMode;
import org.nr31.backend.dto.CalendarEventDTO;
import org.nr31.backend.dto.CreateEventRequest;
import org.nr31.backend.dto.UpdateEventRequest;
import org.nr31.backend.service.CalendarService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/v1/calendar/events")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping
    public ResponseEntity<List<CalendarEventDTO>> getEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        Instant fromInstant = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toInstant = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<CalendarEventDTO> events = calendarService.getEvents(fromInstant, toInstant);
        return ResponseEntity.ok(events);
    }

    @PostMapping
    public ResponseEntity<CalendarEventDTO> createEvent(@RequestBody CreateEventRequest request) {
        CalendarEventDTO created = calendarService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CalendarEventDTO> updateEvent(
            @PathVariable Long id,
            @RequestBody UpdateEventRequest request) {
        CalendarEventDTO updated = calendarService.updateEvent(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long id,
            @RequestParam CalendarActionMode mode,
            @RequestParam(required = false) Instant exceptionDate) {
        calendarService.deleteEvent(id, mode, exceptionDate);
        return ResponseEntity.noContent().build();
    }
}
