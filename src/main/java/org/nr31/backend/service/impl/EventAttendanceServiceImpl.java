package org.nr31.backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import org.dmfs.rfc5545.recur.RecurrenceRuleIterator;
import org.nr31.backend.dto.attendance.EventAttendanceDTO;
import org.nr31.backend.dto.attendance.MemberMonthlyAttendanceDTO;
import org.nr31.backend.exception.CalendarException;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.dto.common.ErrorCode;
import org.nr31.backend.model.AttendanceRecord;
import org.nr31.backend.model.CalendarEvent;
import org.nr31.backend.model.CalendarEventException;
import org.nr31.backend.model.EventAttendance;
import org.nr31.backend.model.MonthlyEventCount;
import org.nr31.backend.model.RosterMember;
import org.nr31.backend.repository.AttendanceRecordRepository;
import org.nr31.backend.repository.CalendarEventExceptionRepository;
import org.nr31.backend.repository.CalendarEventRepository;
import org.nr31.backend.repository.EventAttendanceRepository;
import org.nr31.backend.repository.MonthlyEventCountRepository;
import org.nr31.backend.repository.RosterMemberRepository;
import org.nr31.backend.service.EventAttendanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventAttendanceServiceImpl implements EventAttendanceService {

    private final EventAttendanceRepository eventAttendanceRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final CalendarEventExceptionRepository calendarEventExceptionRepository;
    private final RosterMemberRepository rosterMemberRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final MonthlyEventCountRepository monthlyEventCountRepository;

    private static final int MAX_NUMBER_OF_RECURSIVE_EVENTS = 2000;

    @Override
    @Transactional
    public void recordAttendance(Long eventId, Instant occurrenceDate, List<Long> memberIds) {
        if (!isValidOccurrence(eventId, occurrenceDate)) {
            throw new CalendarException.UserError("Invalid occurrence date for this event");
        }

        if (occurrenceDate.isAfter(Instant.now())) {
            throw new CalendarException.UserError("Cannot record attendance for future events");
        }

        CalendarEvent event = calendarEventRepository.findById(eventId)
                .orElseThrow(() -> new ElementNotFoundException("Event not found", ErrorCode.EVENT_NOT_FOUND));

        eventAttendanceRepository.deleteByEventIdAndOccurrenceDate(eventId, occurrenceDate);

        if (memberIds == null || memberIds.isEmpty()) {
            return;
        }

        List<RosterMember> members = rosterMemberRepository.findAllById(memberIds);
        if (members.size() != memberIds.size()) {
            throw new CalendarException.UserError("One or more members not found");
        }

        List<EventAttendance> newRecords = members.stream()
                .filter(m -> !m.isArchived())
                .map(m -> EventAttendance.builder()
                        .member(m)
                        .event(event)
                        .occurrenceDate(occurrenceDate)
                        .build())
                .collect(Collectors.toList());

        eventAttendanceRepository.saveAll(newRecords);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventAttendanceDTO> getAttendanceForOccurrence(Long eventId, Instant occurrenceDate) {
        return eventAttendanceRepository.findByEventIdAndOccurrenceDate(eventId, occurrenceDate)
                .stream()
                .map(ea -> EventAttendanceDTO.builder()
                        .memberId(ea.getMember().getId())
                        .memberNickname(ea.getMember().getMbNickname())
                        .eventId(ea.getEvent().getId())
                        .eventTitle(ea.getEvent().getTitle())
                        .occurrenceDate(ea.getOccurrenceDate())
                        .createdAt(ea.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isValidOccurrence(Long eventId, Instant occurrenceDate) {
        CalendarEvent event = calendarEventRepository.findById(eventId)
                .orElseThrow(() -> new ElementNotFoundException("Event not found", ErrorCode.EVENT_NOT_FOUND));

        if (event.getRrule() == null || event.getRrule().isEmpty()) {
            return event.getStart().equals(occurrenceDate);
        }

        Optional<CalendarEventException> exception = calendarEventExceptionRepository
                .findByOriginalEventAndExceptionDate(event, occurrenceDate);

        if (exception.isPresent()) {
            return !exception.get().isCancelled();
        }

        try {
            RecurrenceRule rule = new RecurrenceRule(event.getRrule());
            DateTime seed = new DateTime(event.getStart().toEpochMilli());
            RecurrenceRuleIterator it = rule.iterator(seed);
            int sanityCheckLimit = MAX_NUMBER_OF_RECURSIVE_EVENTS;

            while (it.hasNext() && sanityCheckLimit-- > 0) {
                DateTime nextInstance = it.nextDateTime();
                Instant originalStart = Instant.ofEpochMilli(nextInstance.getTimestamp());

                if (originalStart.equals(occurrenceDate)) {
                    return true;
                }

                if (originalStart.isAfter(occurrenceDate)) {
                    break;
                }
            }
        } catch (InvalidRecurrenceRuleException e) {
            log.error("Invalid recurrence rule for event ID: {}", event.getId(), e);
        }

        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public MemberMonthlyAttendanceDTO getMemberMonthlyAttendance(Long memberId, int year, int month) {
        rosterMemberRepository.findById(memberId)
                .orElseThrow(() -> new ElementNotFoundException("Member not found", ErrorCode.ELEMENT_NOT_FOUND));

        YearMonth ym = YearMonth.of(year, month);
        Instant monthStart = ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant monthEnd = ym.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneOffset.UTC).toInstant();

        List<EventAttendance> attendanceRecords = eventAttendanceRepository
                .findByMemberIdAndOccurrenceDateBetween(memberId, monthStart, monthEnd);

        int eventAttendanceScore = attendanceRecords.stream()
                .mapToInt(ea -> ea.getEvent().getType() != null ? ea.getEvent().getType().getAttendanceWeight() : 0)
                .sum();

        Optional<AttendanceRecord> monthlyRecord = attendanceRecordRepository
                .findByMemberIdAndYearAndMonth(memberId, year, month);

        int manualCount = monthlyRecord.map(ar -> ar.getManualAttendanceCount() != null ? ar.getManualAttendanceCount() : 0).orElse(0);

        List<EventAttendanceDTO> dtos = attendanceRecords.stream()
                .map(ea -> EventAttendanceDTO.builder()
                        .memberId(ea.getMember().getId())
                        .memberNickname(ea.getMember().getMbNickname())
                        .eventId(ea.getEvent().getId())
                        .eventTitle(ea.getEvent().getTitle())
                        .occurrenceDate(ea.getOccurrenceDate())
                        .createdAt(ea.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        int maxPossibleScore = calculateMaxPossibleScore(monthStart, monthEnd);
        if (maxPossibleScore == 0) {
            Optional<MonthlyEventCount> mec = monthlyEventCountRepository.findByYearAndMonth(year, month);
            maxPossibleScore = mec.map(MonthlyEventCount::getManualEventCount).orElse(0);
        }

        return MemberMonthlyAttendanceDTO.builder()
                .memberId(memberId)
                .year(year)
                .month(month)
                .eventAttendanceScore(eventAttendanceScore)
                .manualAttendanceCount(manualCount)
                .totalScore(eventAttendanceScore + manualCount)
                .maxPossibleScore(maxPossibleScore)
                .status(monthlyRecord.map(AttendanceRecord::getStatus).orElse(null))
                .attendedEvents(dtos)
                .build();
    }

    private int calculateMaxPossibleScore(Instant monthStart, Instant monthEnd) {
        List<CalendarEvent> allEvents = calendarEventRepository.findEventsInWindowOrRecurring(monthStart, monthEnd);
        int score = 0;
        
        List<Long> eventIds = allEvents.stream().map(CalendarEvent::getId).collect(Collectors.toList());
        List<CalendarEventException> allExceptions = eventIds.isEmpty() ? Collections.emptyList() :
                calendarEventExceptionRepository.findExceptionsForEventsInWindow(
                        eventIds, monthStart.minus(365, ChronoUnit.DAYS), monthEnd.plus(365, ChronoUnit.DAYS));

        for (CalendarEvent event : allEvents) {
            if (event.getType() == null || event.getType().getAttendanceWeight() <= 0) {
                continue;
            }

            int weight = event.getType().getAttendanceWeight();

            if (event.getRrule() == null || event.getRrule().isEmpty()) {
                if (!event.getStart().isBefore(monthStart) && !event.getStart().isAfter(monthEnd)) {
                    score += weight;
                }
            } else {
                List<CalendarEventException> exceptions = allExceptions.stream()
                        .filter(ex -> ex.getOriginalEvent().getId().equals(event.getId()))
                        .toList();
                
                try {
                    RecurrenceRule rule = new RecurrenceRule(event.getRrule());
                    DateTime seed = new DateTime(event.getStart().toEpochMilli());
                    RecurrenceRuleIterator it = rule.iterator(seed);
                    int sanityCheckLimit = MAX_NUMBER_OF_RECURSIVE_EVENTS;

                    while (it.hasNext() && sanityCheckLimit-- > 0) {
                        DateTime nextInstance = it.nextDateTime();
                        Instant originalStart = Instant.ofEpochMilli(nextInstance.getTimestamp());

                        if (originalStart.isAfter(monthEnd)) {
                            break;
                        }

                        Optional<CalendarEventException> exOpt = exceptions.stream()
                                .filter(ex -> ex.getExceptionDate().equals(originalStart))
                                .findFirst();

                        if (exOpt.isPresent()) {
                            CalendarEventException ex = exOpt.get();
                            if (!ex.isCancelled()) {
                                Instant newStart = ex.getNewStart() != null ? ex.getNewStart() : originalStart;
                                if (!newStart.isBefore(monthStart) && !newStart.isAfter(monthEnd)) {
                                    score += weight;
                                }
                            }
                        } else {
                            if (!originalStart.isBefore(monthStart) && !originalStart.isAfter(monthEnd)) {
                                score += weight;
                            }
                        }
                    }
                } catch (InvalidRecurrenceRuleException e) {
                    log.error("Invalid recurrence rule for event ID: {}", event.getId(), e);
                }
            }
        }
        return score;
    }
}
