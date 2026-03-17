package org.nr31.backend.integration.discord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.ScheduledEvent;
import net.dv8tion.jda.api.events.guild.scheduledevent.ScheduledEventCreateEvent;
import net.dv8tion.jda.api.events.guild.scheduledevent.ScheduledEventDeleteEvent;

import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.Route;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import org.nr31.backend.dto.DiscordSyncEventDTO;
import org.nr31.backend.dto.DiscordSyncExceptionDTO;
import org.nr31.backend.service.CalendarService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import net.dv8tion.jda.api.events.guild.scheduledevent.GenericScheduledEventGatewayEvent;
import net.dv8tion.jda.api.events.guild.scheduledevent.update.GenericScheduledEventUpdateEvent;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.Weekday;
import org.dmfs.rfc5545.recur.Freq;
import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException;
import org.dmfs.rfc5545.recur.RecurrenceRule;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordEventListener extends ListenerAdapter {

    protected static final String GUILD_ID_PLACEHOLDER = "";
    protected static final String CALENDAR_CACHE_NAME = "calendarEvents";
    protected static final String SCHEDULED_EVENTS_ROUTE = "guilds/{guild_id}/scheduled-events";

    protected static final String RECURRENCE_RULE_KEY = "recurrence_rule";
    protected static final String GUILD_SCHEDULED_EVENT_EXCEPTIONS_KEY = "guild_scheduled_event_exceptions";
    protected static final String EVENT_EXCEPTION_ID_KEY = "event_exception_id";
    protected static final String IS_CANCELED_KEY = "is_canceled";
    protected static final String FREQUENCY_KEY = "frequency";
    protected static final String INTERVAL_KEY = "interval";
    protected static final String BY_WEEKDAY_KEY = "by_weekday";
    protected static final String BY_N_WEEKDAY_KEY = "by_n_weekday";
    protected static final String N_KEY = "n";
    protected static final String DAY_KEY = "day";
    protected static final String BY_MONTH_KEY = "by_month";
    protected static final String BY_MONTH_DAY_KEY = "by_month_day";
    protected static final String END_KEY = "end";

    protected static final String RAW_ID_KEY = "id";
    protected static final String RAW_NAME_KEY = "name";
    protected static final String RAW_DESCRIPTION_KEY = "description";
    protected static final String RAW_SCHEDULED_START_TIME_KEY = "scheduled_start_time";
    protected static final String RAW_SCHEDULED_END_TIME_KEY = "scheduled_end_time";

    private final CalendarService calendarService;
    private final CacheManager cacheManager;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void onReady(ReadyEvent event) {
        log.info("Discord bot is ready. Running initial event sync...");
        transactionTemplate.executeWithoutResult(status -> {
            try {
                event.getJDA().getGuilds().stream()
                        .filter(g -> g.getId().equals(GUILD_ID_PLACEHOLDER))
                        .findFirst()
                        .ifPresent(guild -> {
                            Route.CompiledRoute route = Route.get(SCHEDULED_EVENTS_ROUTE)
                                    .compile(guild.getId());
                            new RestActionImpl<DataArray>(event.getJDA(), route,
                                    (response, request) -> response.getArray())
                                    .queue(rawEvents -> {
                                        transactionTemplate.executeWithoutResult(s -> {
                                            List<String> syncedServerEventIds = new ArrayList<>();
                                            for (int i = 0; i < rawEvents.length(); i++) {
                                                try {
                                                    DataObject rawEvent = rawEvents.getObject(i);
                                                    syncEventFromRaw(rawEvent);
                                                    syncedServerEventIds.add(rawEvent.getString(RAW_ID_KEY));
                                                } catch (Exception e) {
                                                    log.warn("Failed to sync scheduled event from raw data during onReady", e);
                                                }
                                            }
                                            calendarService.removeOrphanedDiscordEvents(syncedServerEventIds);
                                            evictCache();
                                        });
                                    });
                        });
            } catch (Exception e) {
                log.error("Failed to sync Discord events on startup", e);
            }
        });
    }

    @Override
    public void onGenericScheduledEventGateway(GenericScheduledEventGatewayEvent event) {
        if (event instanceof ScheduledEventCreateEvent || event instanceof GenericScheduledEventUpdateEvent) {
            log.info("Processing Discord Scheduled Event (Create/Update): {}", event.getScheduledEvent().getName());
            transactionTemplate.executeWithoutResult(s -> {
                syncEvent(event.getScheduledEvent(), event.getRawData());
                evictCache();
            });
        } else if (event instanceof ScheduledEventDeleteEvent) {
            log.info("Processing Discord Scheduled Event (Delete): {}", event.getScheduledEvent().getName());
            transactionTemplate.executeWithoutResult(s -> {
                calendarService.deleteDiscordEvent(event.getScheduledEvent().getId());
                evictCache();
            });
        }
    }

    private void syncEvent(ScheduledEvent event, DataObject rawData) {
        DiscordSyncEventDTO.DiscordSyncEventDTOBuilder dtoBuilder = DiscordSyncEventDTO.builder()
                .discordId(event.getId())
                .name(event.getName())
                .description(event.getDescription() != null ? event.getDescription() : "")
                .start(event.getStartTime().toInstant())
                .end(event.getEndTime() != null ? event.getEndTime().toInstant() : null)
                .serverName(event.getGuild().getName());

        if (rawData != null && !rawData.isNull("d")) {
            DataObject data = rawData.getObject("d");

            if (!data.isNull(RECURRENCE_RULE_KEY)) {
                try {
                    String rrule = parseRecurrenceRule(data.getObject(RECURRENCE_RULE_KEY));
                    dtoBuilder.rrule(rrule);
                } catch (Exception e) {
                    log.warn("Could not parse recurrence rule for event {}: {}", event.getId(), e.getMessage());
                }
            }

            if (!data.isNull(GUILD_SCHEDULED_EVENT_EXCEPTIONS_KEY)) {
                DataArray exceptionsArray = data.getArray(GUILD_SCHEDULED_EVENT_EXCEPTIONS_KEY);
                List<DiscordSyncExceptionDTO> exceptionDTOs = new ArrayList<>();
                for (int i = 0; i < exceptionsArray.length(); i++) {
                    DataObject exData = exceptionsArray.getObject(i);
                    DiscordSyncExceptionDTO exDto = parseEventException(exData);
                    if (exDto != null) {
                        exceptionDTOs.add(exDto);
                    }
                }
                dtoBuilder.exceptions(exceptionDTOs);
            }
        }

        calendarService.syncDiscordEvent(dtoBuilder.build());
    }

    private void syncEventFromRaw(DataObject rawEventData) {
        String eventId = rawEventData.getString(RAW_ID_KEY);
        String name = rawEventData.getString(RAW_NAME_KEY, "Unknown Event");
        String description = rawEventData.getString(RAW_DESCRIPTION_KEY, "");
        Instant startTime = OffsetDateTime.parse(rawEventData.getString(RAW_SCHEDULED_START_TIME_KEY)).toInstant();
        Instant endTime = rawEventData.isNull(RAW_SCHEDULED_END_TIME_KEY) ? null
                : OffsetDateTime.parse(rawEventData.getString(RAW_SCHEDULED_END_TIME_KEY)).toInstant();

        DiscordSyncEventDTO.DiscordSyncEventDTOBuilder dtoBuilder = DiscordSyncEventDTO.builder()
                .discordId(eventId)
                .name(name)
                .description(description)
                .start(startTime)
                .end(endTime);

        if (!rawEventData.isNull(RECURRENCE_RULE_KEY)) {
            try {
                String rrule = parseRecurrenceRule(rawEventData.getObject(RECURRENCE_RULE_KEY));
                dtoBuilder.rrule(rrule);
            } catch (Exception e) {
                log.warn("Could not parse recurrence rule for event {}: {}", eventId, e.getMessage());
            }
        }

        if (!rawEventData.isNull(GUILD_SCHEDULED_EVENT_EXCEPTIONS_KEY)) {
            DataArray exceptionsArray = rawEventData.getArray(GUILD_SCHEDULED_EVENT_EXCEPTIONS_KEY);
            List<DiscordSyncExceptionDTO> exceptionDTOs = new ArrayList<>();
            for (int i = 0; i < exceptionsArray.length(); i++) {
                DataObject exData = exceptionsArray.getObject(i);
                DiscordSyncExceptionDTO exDto = parseEventException(exData);
                if (exDto != null) {
                    exceptionDTOs.add(exDto);
                }
            }
            dtoBuilder.exceptions(exceptionDTOs);
        }

        calendarService.syncDiscordEvent(dtoBuilder.build());
    }

    private DiscordSyncExceptionDTO parseEventException(DataObject exData) {
        try {
            String exceptionId = exData.getString("event_exception_id");
            boolean isCancelled = exData.getBoolean("is_canceled", false);

            DiscordSyncExceptionDTO.DiscordSyncExceptionDTOBuilder dtoBuilder = DiscordSyncExceptionDTO.builder()
                    .exceptionId(exceptionId)
                    .isCancelled(isCancelled);

            if (!exData.isNull(RAW_SCHEDULED_START_TIME_KEY)) {
                dtoBuilder.newStart(OffsetDateTime.parse(exData.getString(RAW_SCHEDULED_START_TIME_KEY)).toInstant());
            }
            if (!exData.isNull(RAW_SCHEDULED_END_TIME_KEY)) {
                dtoBuilder.newEnd(OffsetDateTime.parse(exData.getString(RAW_SCHEDULED_END_TIME_KEY)).toInstant());
            }

            dtoBuilder.exceptionDate(dtoBuilder.build().getNewStart());
            
            if (dtoBuilder.build().getExceptionDate() == null) {
                dtoBuilder.exceptionDate(Instant.now());
            }

            return dtoBuilder.build();
        } catch (Exception e) {
            log.warn("Failed to parse event exception: {}", e.getMessage());
            return null;
        }
    }

    private String parseRecurrenceRule(DataObject rule) throws InvalidRecurrenceRuleException {
        int freqInt = rule.getInt(FREQUENCY_KEY, 2);
        Freq freq;
        switch (freqInt) {
            case 0 -> freq = Freq.YEARLY;
            case 1 -> freq = Freq.MONTHLY;
            case 3 -> freq = Freq.DAILY;
            default -> freq = Freq.WEEKLY;
        }

        RecurrenceRule rrule = new RecurrenceRule(freq);

        if (!rule.isNull(INTERVAL_KEY)) {
            int interval = rule.getInt(INTERVAL_KEY);
            if (interval > 1) {
                rrule.setInterval(interval);
            }
        }

        List<RecurrenceRule.WeekdayNum> byDayList = new ArrayList<>();

        if (!rule.isNull(BY_WEEKDAY_KEY)) {
            DataArray byWeekdayArray = rule.getArray(BY_WEEKDAY_KEY);
            for (int i = 0; i < byWeekdayArray.length(); i++) {
                int discordDay = byWeekdayArray.getInt(i);
                byDayList.add(new RecurrenceRule.WeekdayNum(0, getWeekday(discordDay)));
            }
        }

        if (!rule.isNull(BY_N_WEEKDAY_KEY)) {
            DataArray byNWeekdayArray = rule.getArray(BY_N_WEEKDAY_KEY);
            for (int i = 0; i < byNWeekdayArray.length(); i++) {
                DataObject nWeekdayObj = byNWeekdayArray.getObject(i);
                int discordDay = nWeekdayObj.getInt(DAY_KEY);
                int n = nWeekdayObj.getInt(N_KEY);
                byDayList.add(new RecurrenceRule.WeekdayNum(n, getWeekday(discordDay)));
            }
        }

        if (!byDayList.isEmpty()) {
            rrule.setByDayPart(byDayList);
        }

        if (!rule.isNull(BY_MONTH_KEY)) {
            DataArray byMonthArray = rule.getArray(BY_MONTH_KEY);
            Integer[] months = new Integer[byMonthArray.length()];
            for (int i = 0; i < byMonthArray.length(); i++) {
                months[i] = byMonthArray.getInt(i);
            }
            rrule.setByPart(RecurrenceRule.Part.BYMONTH, months);
        }

        if (!rule.isNull(BY_MONTH_DAY_KEY)) {
            DataArray byMonthDayArray = rule.getArray(BY_MONTH_DAY_KEY);
            Integer[] monthDays = new Integer[byMonthDayArray.length()];
            for (int i = 0; i < byMonthDayArray.length(); i++) {
                monthDays[i] = byMonthDayArray.getInt(i);
            }
            rrule.setByPart(RecurrenceRule.Part.BYMONTHDAY, monthDays);
        }

        if (!rule.isNull(END_KEY)) {
            try {
                Instant untilInstant = OffsetDateTime.parse(rule.getString(END_KEY)).toInstant();
                rrule.setUntil(new DateTime(untilInstant.toEpochMilli()));
            } catch (Exception e) {
                log.warn("Could not parse recurrence end date field");
            }
        }

        return rrule.toString();
    }

    private Weekday getWeekday(int discordDay) {
        return switch (discordDay) {
            case 0 -> Weekday.MO;
            case 1 -> Weekday.TU;
            case 2 -> Weekday.WE;
            case 3 -> Weekday.TH;
            case 4 -> Weekday.FR;
            case 5 -> Weekday.SA;
            case 6 -> Weekday.SU;
            default -> Weekday.MO;
        };
    }

    private void evictCache() {
        Cache cache = cacheManager.getCache(CALENDAR_CACHE_NAME);
        if (cache != null) {
            cache.clear();
        }
    }
}
