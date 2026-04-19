package org.nr31.backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.Weekday;
import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import org.dmfs.rfc5545.recur.RecurrenceRuleIterator;
import org.dmfs.rfc5545.recur.Freq;
import org.hibernate.Hibernate;
import org.nr31.backend.dto.CalendarActionMode;
import org.nr31.backend.dto.CalendarEventDTO;
import org.nr31.backend.dto.CreateEventRequest;
import org.nr31.backend.dto.DiscordSyncEventDTO;
import org.nr31.backend.dto.DiscordSyncExceptionDTO;
import org.nr31.backend.dto.ErrorCode;
import org.nr31.backend.dto.EventTypeDTO;
import org.nr31.backend.dto.Recurrence;
import org.nr31.backend.dto.UnitTypeDTO;
import org.nr31.backend.dto.UpdateEventRequest;
import org.nr31.backend.exception.CalendarException;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.integration.discord.EventSource;
import org.nr31.backend.model.CalendarEvent;
import org.nr31.backend.model.CalendarEventException;
import org.nr31.backend.model.EventType;
import org.nr31.backend.model.UnitType;
import org.nr31.backend.repository.CalendarEventExceptionRepository;
import org.nr31.backend.repository.CalendarEventRepository;
import org.nr31.backend.repository.EventTypeRepository;
import org.nr31.backend.repository.SupportedLocaleRepository;
import org.nr31.backend.repository.UnitTypeRepository;
import org.nr31.backend.service.CalendarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarServiceImpl implements CalendarService {

    public static final int MAX_NUMBER_OF_RECURSIVE_EVENTS = 2000;
    private final CalendarEventRepository calendarEventRepository;
    private final CalendarEventExceptionRepository calendarEventExceptionRepository;
    private final EventTypeRepository eventTypeRepository;
    private final SupportedLocaleRepository supportedLocaleRepository;
    private final UnitTypeRepository unitTypeRepository;

    @Lazy
    @Autowired
    private CalendarService self;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "calendarEvents", key = "{#from.toEpochMilli(), #to.toEpochMilli(), #targetZone.id}")
    public List<CalendarEventDTO> getEvents(Instant from, Instant to, ZoneId targetZone) {
        List<CalendarEvent> allEvents = calendarEventRepository.findEventsInWindowOrRecurring(from, to);
        List<Long> recursiveEventsIds = allEvents.stream()
                .filter(e -> e.getRrule() != null)
                .map(CalendarEvent::getId)
                .toList();

        Map<Long, List<CalendarEventException>> exceptionsByEventId = new HashMap<>();
        if (!recursiveEventsIds.isEmpty()) {
            List<CalendarEventException> exceptions = calendarEventExceptionRepository
                    .findExceptionsForEventsInWindow(recursiveEventsIds, from.minus(Duration.ofDays(365)),
                            to.plus(Duration.ofDays(365)));
            exceptionsByEventId = exceptions.stream()
                    .collect(Collectors.groupingBy(ex -> ex.getOriginalEvent().getId()));
        }

        List<CalendarEventDTO> result = new ArrayList<>();

        for (CalendarEvent event : allEvents) {
            if (event.getRrule() == null || event.getRrule().isEmpty()) {
                if (event.getStart().isBefore(to) && event.getEnd().isAfter(from)) {
                    result.add(convertToDTO(event, event.getStart(), event.getEnd(), false, targetZone));
                }
            } else {
                List<CalendarEventException> exceptions = exceptionsByEventId.getOrDefault(event.getId(),
                        Collections.emptyList());
                processRecursiveEvent(from, to, event, exceptions, result, targetZone);
            }
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CalendarEventDTO> getNearestEvent(Instant targetDate, ZoneId targetZone) {
        ZonedDateTime targetZdt = targetDate.atZone(targetZone);
        Instant windowStart = targetZdt.truncatedTo(ChronoUnit.DAYS).toInstant();

        Instant windowEnd = windowStart.plus(Duration.ofDays(31));

        List<CalendarEventDTO> events = self.getEvents(windowStart, windowEnd, targetZone);

        CalendarEventDTO nearest = null;
        long minDistance = Long.MAX_VALUE;

        for (CalendarEventDTO event : events) {
            Instant start = event.getStart().toInstant();
            Instant end = event.getEnd().toInstant();

            if (end.isBefore(targetDate)) {
                continue;
            }

            long distance;
            if (!start.isAfter(targetDate)) {
                distance = 0;
            } else {
                distance = Duration.between(targetDate, start).toMillis();
            }

            if (distance < minDistance) {
                minDistance = distance;
                nearest = event;
            }
        }

        return Optional.ofNullable(nearest);
    }

    private void processRecursiveEvent(Instant windowStart, Instant windowEnd, CalendarEvent event,
            List<CalendarEventException> exceptions, List<CalendarEventDTO> result, ZoneId targetZone) {
        try {
            RecurrenceRule rule = new RecurrenceRule(event.getRrule());
            DateTime seed = new DateTime(event.getStart().toEpochMilli());

            ZoneId zoneId = (event.getTimezone() != null) ? ZoneId.of(event.getTimezone()) : ZoneId.systemDefault();
            Duration duration = Duration.between(event.getStart(), event.getEnd());

            RecurrenceRuleIterator it = rule.iterator(seed);
            int sanityCheckLimit = MAX_NUMBER_OF_RECURSIVE_EVENTS;

            while (it.hasNext() && sanityCheckLimit-- > 0) {
                DateTime nextInstance = it.nextDateTime();
                Instant originalStart = Instant.ofEpochMilli(nextInstance.getTimestamp());

                if (originalStart.isAfter(windowEnd)) {
                    break;
                }

                Optional<CalendarEventException> exOpt = exceptions.stream()
                        .filter(ex -> ex.getExceptionDate().equals(originalStart))
                        .findFirst();

                if (exOpt.isPresent()) {
                    CalendarEventException ex = exOpt.get();
                    Instant newStart = ex.getNewStart() != null ? ex.getNewStart() : originalStart;
                    Instant newEnd = ex.getNewEnd() != null ? ex.getNewEnd() : newStart.plus(duration);

                    if (newStart.isBefore(windowEnd) && newEnd.isAfter(windowStart)) {
                        result.add(convertToDTOWithException(event, ex, newStart, newEnd, targetZone));
                    }
                } else {
                    ZonedDateTime zdtStart = originalStart.atZone(zoneId);
                    Instant instanceStart = zdtStart.toInstant();
                    Instant instanceEnd = instanceStart.plus(duration);

                    if (instanceStart.isBefore(windowEnd) && instanceEnd.isAfter(windowStart)) {
                        result.add(convertToDTO(event, instanceStart, instanceEnd, true, targetZone));
                    }
                }
            }

        } catch (InvalidRecurrenceRuleException e) {
            log.error("Invalid recurrence rule for event ID: {}", event.getId(), e);
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "calendarEvents", allEntries = true)
    public CalendarEventDTO createEvent(CreateEventRequest request) {
        String rruleString = buildRRuleString(request.getRecurrence());

        EventType type = eventTypeRepository.findById(request.getType())
                .orElseThrow(() -> new CalendarException.UserError("Event type not found"));

        List<UnitType> units = new ArrayList<>();
        if (request.getParticipatingUnits() != null) {
            units = unitTypeRepository.findAllById(request.getParticipatingUnits());
        }

        String timezoneStr = request.getStart() != null ? request.getStart().getOffset().getId() : "Z";

        CalendarEvent event = CalendarEvent.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .start(request.getStart() != null ? request.getStart().toInstant() : null)
                .end(request.getEnd() != null ? request.getEnd().toInstant() : null)
                .type(type)
                .timezone(timezoneStr)
                .serverName(request.getServerName())
                .participatingUnits(units)
                .rrule(rruleString)
                .seriesId(rruleString != null ? UUID.randomUUID().toString() : null)
                .build();

        event = calendarEventRepository.save(event);
        return convertToDTO(event, event.getStart(), event.getEnd(), rruleString != null, ZoneId.of(timezoneStr));
    }

    @Override
    @Transactional
    @CacheEvict(value = "calendarEvents", allEntries = true)
    public CalendarEventDTO updateEvent(Long id, UpdateEventRequest request) {
        CalendarEvent originalEvent = calendarEventRepository.findById(id)
                .orElseThrow(() -> new ElementNotFoundException("Event not found", ErrorCode.EVENT_NOT_FOUND));

        Instant requestStart = request.getStart() != null ? request.getStart().toInstant() : null;
        Instant requestEnd = request.getEnd() != null ? request.getEnd().toInstant() : null;

        EventType type = request.getType() != null ? eventTypeRepository.findById(request.getType())
                .orElseThrow(() -> new CalendarException.UserError("Event type not found"))
                : originalEvent.getType();

        List<UnitType> unitTypes = request.getParticipatingUnits() != null
                ? unitTypeRepository.findAllById(request.getParticipatingUnits())
                : originalEvent.getParticipatingUnits();

        switch (request.getMode()) {
            case SINGLE -> {
                Instant targetInstanceStart = request.getOriginalStart() != null
                        ? request.getOriginalStart().toInstant()
                        : null;
                return updateSingleEvent(request, originalEvent, targetInstanceStart, requestStart, requestEnd, type,
                        unitTypes);
            }
            case ALL -> {
                return updateAllEvents(request, originalEvent, requestStart, requestEnd, type);
            }
            case FUTURE -> {
                return updateFutureEvents(request, originalEvent, requestStart, type.getId());
            }
            default -> throw new CalendarException.UserError("Invalid mode");
        }
    }

    private CalendarEventDTO updateFutureEvents(UpdateEventRequest request, CalendarEvent originalEvent,
            Instant splitDate, Long typeId) {
        if (EventSource.DISCORD.equals(originalEvent.getSource())) {
            throw new IllegalArgumentException(String.format("Unable to update events with source %s in %s mode", EventSource.DISCORD, CalendarActionMode.FUTURE));
        }
        if (originalEvent.getRrule() != null) {
            Instant untilTimestamp = splitDate.minusSeconds(1);
            stopEventAt(originalEvent, new DateTime(untilTimestamp.toEpochMilli()));

            CreateEventRequest createRequest = convertUpdateRequestToCreate(request, typeId,
                    originalEvent.getParticipatingUnits().stream().map(UnitType::getId).toList());

            CalendarEventDTO newEventDto = createEvent(createRequest);

            List<CalendarEventException> futureExceptions = calendarEventExceptionRepository
                    .findByOriginalEvent(originalEvent).stream()
                    .filter(ex -> !ex.getExceptionDate().isBefore(splitDate))
                    .toList();

            CalendarEvent newEvent = calendarEventRepository.findById(Long.parseLong(newEventDto.getId()))
                    .orElseThrow(() -> new CalendarException.ServerError("Failed to retrieve newly created event"));

            for (CalendarEventException ex : futureExceptions) {
                ex.setOriginalEvent(newEvent);
                calendarEventExceptionRepository.save(ex);
            }

            return newEventDto;
        }

        throw new CalendarException.UserError("Unable to update future occurrences of a non-recursive event");
    }

    private void stopEventAt(CalendarEvent originalEvent, DateTime untilTime) {
        try {
            RecurrenceRule oldRule = new RecurrenceRule(originalEvent.getRrule());
            oldRule.setUntil(untilTime);
            originalEvent.setRrule(oldRule.toString());
            calendarEventRepository.save(originalEvent);
        } catch (InvalidRecurrenceRuleException e) {
            throw new CalendarException.ServerError("Unable to change recursive rule of event");
        }
    }

    private CalendarEventDTO updateAllEvents(UpdateEventRequest request, CalendarEvent originalEvent,
            Instant requestStart, Instant requestEnd, EventType type) {
        if (originalEvent.getSource() != EventSource.DISCORD) {
            originalEvent.setTitle(request.getTitle());
            originalEvent.setDescription(request.getDescription());
            originalEvent.setStart(requestStart);
            originalEvent.setEnd(requestEnd);

            if (request.getRecurrence() != null) {
                originalEvent.setRrule(buildRRuleString(request.getRecurrence()));
                originalEvent.setSeriesId(UUID.randomUUID().toString());
            } else {
                originalEvent.setRrule(null);
                originalEvent.setSeriesId(null);
            }
        }

        originalEvent.setType(type);
        originalEvent.setServerName(request.getServerName());
        List<UnitType> unitTypes = request.getParticipatingUnits() != null
                ? unitTypeRepository.findAllById(request.getParticipatingUnits())
                : originalEvent.getParticipatingUnits();
        originalEvent.setParticipatingUnits(unitTypes);

        originalEvent = calendarEventRepository.save(originalEvent);
        calendarEventExceptionRepository.deleteByOriginalEvent(originalEvent);

        return convertToDTO(originalEvent, originalEvent.getStart(), originalEvent.getEnd(),
                originalEvent.getRrule() != null, ZoneId.of(originalEvent.getTimezone()));
    }

    private CalendarEventDTO updateSingleEvent(UpdateEventRequest request, CalendarEvent originalEvent,
            Instant exceptionDate, Instant newStart, Instant newEnd, EventType type, List<UnitType> unitTypes) {
        if (originalEvent.getRrule() == null || originalEvent.getRrule().isEmpty()) {
            if (originalEvent.getSource() != EventSource.DISCORD) {
                originalEvent.setTitle(request.getTitle());
                originalEvent.setDescription(request.getDescription());
                originalEvent.setStart(newStart);
                originalEvent.setEnd(newEnd);
                String rruleString = buildRRuleString(request.getRecurrence());
                originalEvent.setRrule(rruleString);
                originalEvent.setSeriesId(UUID.randomUUID().toString());
            }
            originalEvent.setType(type);
            originalEvent.setParticipatingUnits(unitTypes);
            calendarEventRepository.save(originalEvent);
            return convertToDTO(originalEvent, newStart, newEnd, originalEvent.getRrule() != null, ZoneId.of(originalEvent.getTimezone()));
        } else {
            CalendarEventException ex = calendarEventExceptionRepository
                    .findByOriginalEventAndExceptionDate(originalEvent, exceptionDate)
                    .orElseGet(() -> CalendarEventException.builder()
                            .originalEvent(originalEvent)
                            .exceptionDate(exceptionDate)
                            .build());

            ex.setCancelled(false);
            if (originalEvent.getSource() != EventSource.DISCORD) {
                ex.setNewTitle(request.getTitle());
                ex.setNewDescription(request.getDescription());
                ex.setNewStart(newStart);
                ex.setNewEnd(newEnd);
            }
            ex.setNewType(type);
            ex.setNewServerName(request.getServerName());
            ex.setNewParticipatingUnits(unitTypes != null ? new ArrayList<>(unitTypes) : null);

            calendarEventExceptionRepository.save(ex);
            return convertToDTOWithException(originalEvent, ex, newStart, newEnd,
                    ZoneId.of(originalEvent.getTimezone()));
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "calendarEvents", allEntries = true)
    public void deleteEvent(Long id, CalendarActionMode mode, Instant exceptionDate) {
        CalendarEvent originalEvent = calendarEventRepository.findById(id)
                .orElseThrow(() -> new ElementNotFoundException("Event not found", ErrorCode.EVENT_NOT_FOUND));

        if (originalEvent.getSource() == EventSource.DISCORD) {
            throw new CalendarException.UserError("Unable to delete event synced from Discord server");
        }

        if (originalEvent.getRrule() == null || originalEvent.getRrule().isEmpty()) {
            calendarEventExceptionRepository.deleteByOriginalEvent(originalEvent);
            calendarEventRepository.delete(originalEvent);
            return;
        }

        switch (mode) {
            case SINGLE -> {
                if (exceptionDate == null) {
                    throw new CalendarException.UserError("exceptionDate is required for SINGLE mode deletion");
                }
                CalendarEventException ex = calendarEventExceptionRepository
                        .findByOriginalEventAndExceptionDate(originalEvent, exceptionDate)
                        .orElseGet(() -> CalendarEventException.builder()
                                .originalEvent(originalEvent)
                                .exceptionDate(exceptionDate)
                                .build());
                ex.setCancelled(true);
                calendarEventExceptionRepository.save(ex);
            }
            case ALL -> {
                calendarEventExceptionRepository.deleteByOriginalEvent(originalEvent);
                calendarEventRepository.delete(originalEvent);
            }
            case FUTURE -> {
                if (exceptionDate == null)
                    throw new CalendarException.UserError("Date required");
                stopEventAt(originalEvent, new DateTime(exceptionDate.minusSeconds(1).toEpochMilli()));
            }
            default -> throw new CalendarException.UserError("Invalid mode");
        }
    }

    private CreateEventRequest convertUpdateRequestToCreate(UpdateEventRequest req, Long type, List<Long> units) {
        CreateEventRequest res = new CreateEventRequest();
        res.setTitle(req.getTitle());
        res.setDescription(req.getDescription());
        res.setStart(req.getStart());
        res.setEnd(req.getEnd());
        res.setType(type);
        res.setParticipatingUnits(units);
        res.setServerName(req.getServerName());
        res.setRecurrence(req.getRecurrence());
        return res;
    }

    private String buildRRuleString(Recurrence rec) {
        if (rec == null || rec.getFrequency() == null)
            return null;
        try {
            Freq f = Freq.valueOf(rec.getFrequency().toUpperCase());
            RecurrenceRule rrule = new RecurrenceRule(f);

            if (rec.getInterval() != null && rec.getInterval() > 1) {
                rrule.setInterval(rec.getInterval());
            }
            if (rec.getCount() != null) {
                rrule.setCount(rec.getCount());
            } else if (rec.getUntil() != null) {
                rrule.setUntil(new DateTime(Instant.parse(rec.getUntil()).toEpochMilli()));
            }

            if (rec.getByDay() != null && !rec.getByDay().isEmpty()) {
                List<RecurrenceRule.WeekdayNum> byDayList = new ArrayList<>();
                for (String w : rec.getByDay()) {
                    Weekday dw = Weekday.valueOf(w.toUpperCase());
                    byDayList.add(new RecurrenceRule.WeekdayNum(0, dw));
                }
                rrule.setByDayPart(byDayList);
            }

            return rrule.toString();
        } catch (Exception e) {
            log.error("Failed to build RRule", e);
            throw new CalendarException.UserError("Invalid recurrence parameters");
        }
    }

    private CalendarEventDTO convertToDTO(CalendarEvent event, Instant actualStart, Instant actualEnd,
            boolean isRecurring, ZoneId targetZone) {
        CalendarEventDTO dto = new CalendarEventDTO();
        dto.setId(event.getId().toString());
        dto.setSeriesId(event.getSeriesId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setStart(java.time.OffsetDateTime.ofInstant(actualStart, targetZone));
        dto.setEnd(java.time.OffsetDateTime.ofInstant(actualEnd, targetZone));
        dto.setType(mapToEventTypeDTO(event.getType()));
        dto.setServerName(event.getServerName());
        dto.setParticipatingUnits(mapToUnitTypeDTOList(event.getParticipatingUnits()));
        dto.setRecurring(isRecurring);
        dto.setSource(event.getSource());
        dto.setDiscordId(event.getDiscordId());
        dto.setCancelled(false);
        return dto;
    }

    private CalendarEventDTO convertToDTOWithException(CalendarEvent event, CalendarEventException ex,
                                                       Instant start, Instant end, ZoneId targetZone) {
        CalendarEventDTO dto = new CalendarEventDTO();
        dto.setId(event.getId().toString());
        dto.setSeriesId(event.getSeriesId());
        dto.setTitle(ex.getNewTitle() != null ? ex.getNewTitle() : event.getTitle());
        dto.setDescription(ex.getNewDescription() != null ? ex.getNewDescription() : event.getDescription());
        dto.setStart(java.time.OffsetDateTime.ofInstant(start, targetZone));
        dto.setEnd(java.time.OffsetDateTime.ofInstant(end, targetZone));

        EventType type = ex.getNewType() != null ? ex.getNewType() : event.getType();
        dto.setType(mapToEventTypeDTO(type));

        dto.setServerName(ex.getNewServerName() != null ? ex.getNewServerName() : event.getServerName());
        List<UnitType> units = (ex.getNewParticipatingUnits() != null && !ex.getNewParticipatingUnits().isEmpty())
                ? ex.getNewParticipatingUnits() : event.getParticipatingUnits();
        dto.setParticipatingUnits(mapToUnitTypeDTOList(units));

        dto.setRecurring(true);
        dto.setSource(event.getSource());
        dto.setDiscordId(event.getDiscordId());
        dto.setCancelled(ex.isCancelled());
        return dto;
    }

    private EventTypeDTO mapToEventTypeDTO(EventType entity) {
        if (entity == null) {
            return null;
        }
        Hibernate.initialize(entity);
        return EventTypeDTO.builder()
                .id(entity.getId())
                .name(new HashMap<>(entity.getName()))
                .customIcon(entity.getCustomIcon() != null ? entity.getCustomIcon().getId() : null)
                .build();
    }

    private List<UnitTypeDTO> mapToUnitTypeDTOList(List<UnitType> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        Hibernate.initialize(entities);
        return entities.stream()
                .map(this::mapToUnitTypeDTO)
                .collect(Collectors.toList());
    }

    private UnitTypeDTO mapToUnitTypeDTO(UnitType entity) {
        if (entity == null) {
            return null;
        }
        return UnitTypeDTO.builder()
                .id(entity.getId())
                .name(new HashMap<>(entity.getName()))
                .description(entity.getDescription() != null ? new HashMap<>(entity.getDescription()) : null)
                .build();
    }

    @Override
    @Transactional
    public void syncDiscordEvent(DiscordSyncEventDTO dto) {
        CalendarEvent event = calendarEventRepository.findByDiscordId(dto.getDiscordId())
                .orElseGet(() -> {
                    CalendarEvent newEvent = new CalendarEvent();
                    newEvent.setDiscordId(dto.getDiscordId());
                    newEvent.setSource(EventSource.DISCORD);
                    newEvent.setSeriesId(UUID.randomUUID().toString());
                    //TODO resolve dynamically
                    eventTypeRepository.findById(1L).ifPresent(newEvent::setType);
                    return newEvent;
                });

        Map<String, String> names = new HashMap<>();
        Map<String, String> descriptions = new HashMap<>();

        supportedLocaleRepository.findAll().forEach(locale -> {
            names.put(locale.getCode(), dto.getName());
            if (dto.getDescription() != null && !dto.getDescription().isEmpty()) {
                descriptions.put(locale.getCode(), dto.getDescription());
            }
        });

        event.setTitle(names);
        event.setDescription(descriptions);
        event.setStart(dto.getStart());
        event.setEnd(dto.getEnd() != null ? dto.getEnd() : dto.getStart().plus(Duration.ofHours(1)));

        event.setServerName(dto.getServerName() != null ? dto.getServerName() : "Discord");
        event.setRrule(dto.getRrule());
        event.setTimezone(dto.getTimezone() != null ? dto.getTimezone() : "Z");

        final CalendarEvent savedEvent = calendarEventRepository.save(event);

        if (dto.getExceptions() != null && !dto.getExceptions().isEmpty()) {
            for (DiscordSyncExceptionDTO exDto : dto.getExceptions()) {
                CalendarEventException exception = calendarEventExceptionRepository
                        .findByDiscordExceptionId(exDto.getExceptionId())
                        .orElseGet(() -> {
                            CalendarEventException ex = new CalendarEventException();
                            ex.setDiscordExceptionId(exDto.getExceptionId());
                            ex.setOriginalEvent(savedEvent);
                            return ex;
                        });

                exception.setOriginalEvent(savedEvent);
                exception.setCancelled(exDto.isCancelled());
                exception.setExceptionDate(exDto.getExceptionDate());
                exception.setNewStart(exDto.getNewStart());
                exception.setNewEnd(exDto.getNewEnd());

                calendarEventExceptionRepository.save(exception);
            }
        }
    }

    @Override
    @Transactional
    public void syncDiscordEventException(String discordId, DiscordSyncExceptionDTO exDto) {
        calendarEventRepository.findByDiscordId(discordId).ifPresent(event -> {
            CalendarEventException exception = calendarEventExceptionRepository
                    .findByDiscordExceptionId(exDto.getExceptionId())
                    .orElseGet(() -> {
                        CalendarEventException ex = new CalendarEventException();
                        ex.setDiscordExceptionId(exDto.getExceptionId());
                        return ex;
                    });

            exception.setOriginalEvent(event);
            exception.setCancelled(exDto.isCancelled());
            exception.setExceptionDate(exDto.getExceptionDate());
            exception.setNewStart(exDto.getNewStart());
            exception.setNewEnd(exDto.getNewEnd());

            calendarEventExceptionRepository.save(exception);
            log.info("Synced discord event exception for event discord ID: {}", discordId);
        });
    }

    @Override
    @Transactional
    public void deleteDiscordEventException(String exceptionId) {
        calendarEventExceptionRepository.findByDiscordExceptionId(exceptionId).ifPresent(ex -> {
            calendarEventExceptionRepository.delete(ex);
            log.info("Deleted discord event exception with ID: {}", exceptionId);
        });
    }

    @Override
    @Transactional
    public void removeOrphanedDiscordEvents(List<String> activeDiscordIds) {
        Instant now = Instant.now();
        List<CalendarEvent> allFutureDiscordEvents = calendarEventRepository.findBySourceAndEndAfter(EventSource.DISCORD, now);

        for (CalendarEvent event : allFutureDiscordEvents) {
            if (event.getDiscordId() != null && !activeDiscordIds.contains(event.getDiscordId())) {
                calendarEventExceptionRepository.deleteByOriginalEvent(event);
                calendarEventRepository.delete(event);
                log.info("Removed orphaned Discord event: {}", event.getTitle());
            }
        }
    }

    @Override
    @Transactional
    public void deleteDiscordEvent(String discordId) {
        calendarEventRepository.findByDiscordId(discordId)
                .ifPresent(event -> {
                    calendarEventExceptionRepository.deleteByOriginalEvent(event);
                    calendarEventRepository.delete(event);
                    log.info("Deleted Discord event via API: {}", event.getTitle());
                });
    }
}
