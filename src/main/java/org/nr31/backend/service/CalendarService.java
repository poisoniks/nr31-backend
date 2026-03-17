package org.nr31.backend.service;

import org.nr31.backend.dto.CalendarActionMode;
import org.nr31.backend.dto.CalendarEventDTO;
import org.nr31.backend.dto.CreateEventRequest;
import org.nr31.backend.dto.DiscordSyncEventDTO;
import org.nr31.backend.dto.UpdateEventRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.time.ZoneId;

public interface CalendarService {

    List<CalendarEventDTO> getEvents(Instant from, Instant to, ZoneId targetZone);

    Optional<CalendarEventDTO> getNearestEvent(Instant targetDate, ZoneId targetZone);

    CalendarEventDTO createEvent(CreateEventRequest request);

    CalendarEventDTO updateEvent(Long id, UpdateEventRequest request);

    void deleteEvent(Long id, CalendarActionMode mode, Instant exceptionDate);

    void syncDiscordEvent(DiscordSyncEventDTO dto);

    void removeOrphanedDiscordEvents(List<String> activeDiscordIds);

    void deleteDiscordEvent(String discordId);
}
