package org.nr31.backend.integration.discord;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.RawGatewayEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.jetbrains.annotations.NotNull;
import org.nr31.backend.integration.discord.dto.DiscordDeleteDTO;
import org.nr31.backend.integration.discord.dto.DiscordEventExceptionDTO;
import org.nr31.backend.integration.discord.dto.DiscordScheduledEventDTO;
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
public abstract class UpdatedListenerAdapter extends ListenerAdapter {

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
    protected static final String GUILD_ID_KEY = "guild_id";
    protected static final String EVENT_ID_KEY = "event_id";

    @Override
    public void onRawGateway(@NotNull RawGatewayEvent raw) {
        switch (raw.getType()) {
            case "GUILD_SCHEDULED_EVENT_CREATE" -> {
                onScheduleEventCreated(parseEvent(raw.getPayload(), raw.getJDA()));
            }
            case "GUILD_SCHEDULED_EVENT_UPDATE" -> {
                onScheduleEventUpdated(parseEvent(raw.getPayload(), raw.getJDA()));
            }
            case "GUILD_SCHEDULED_EVENT_DELETE" -> {
                onScheduleEventDeleted(parseEventDelete(raw.getPayload()));
            }
            case "GUILD_SCHEDULED_EVENT_EXCEPTION_CREATE" -> {
                onScheduleEventExceptionCreated(parseException(raw.getPayload()));
            }
            case "GUILD_SCHEDULED_EVENT_EXCEPTION_UPDATE" -> {
                onScheduleEventExceptionUpdated(parseException(raw.getPayload()));
            }
            case "GUILD_SCHEDULED_EVENT_EXCEPTION_DELETE" -> {
                onScheduleEventExceptionDeleted(parseExceptionDelete(raw.getPayload()));
            }
        }
    }

    protected void onScheduleEventCreated(DiscordScheduledEventDTO event) {}

    protected void onScheduleEventUpdated(DiscordScheduledEventDTO event) {}

    protected void onScheduleEventDeleted(DiscordDeleteDTO event) {}

    protected void onScheduleEventExceptionCreated(DiscordEventExceptionDTO exception) {}

    protected void onScheduleEventExceptionUpdated(DiscordEventExceptionDTO exception) {}

    protected void onScheduleEventExceptionDeleted(DiscordDeleteDTO exception) {}

    protected DiscordScheduledEventDTO parseEvent(DataObject rawEventData, JDA jda) {
        String eventId = rawEventData.getString(RAW_ID_KEY);
        String guildId = rawEventData.isNull(GUILD_ID_KEY) ? null : rawEventData.getString(GUILD_ID_KEY);
        String name = rawEventData.getString(RAW_NAME_KEY, "Unknown Event");
        String description = rawEventData.isNull(RAW_DESCRIPTION_KEY) ? ""
                : rawEventData.getString(RAW_DESCRIPTION_KEY, "");
        Instant startTime = OffsetDateTime.parse(rawEventData.getString(RAW_SCHEDULED_START_TIME_KEY)).toInstant();
        Instant endTime = rawEventData.isNull(RAW_SCHEDULED_END_TIME_KEY) ? null
                : OffsetDateTime.parse(rawEventData.getString(RAW_SCHEDULED_END_TIME_KEY)).toInstant();

        String serverName = "Unknown Server";
        if (jda != null && guildId != null) {
            Guild guild = jda.getGuildById(guildId);
            if (guild != null) {
                serverName = guild.getName();
            }
        }

        DiscordScheduledEventDTO.DiscordScheduledEventDTOBuilder builder = DiscordScheduledEventDTO.builder()
                .id(eventId)
                .guildId(guildId)
                .name(name)
                .description(description)
                .scheduledStartTime(startTime)
                .scheduledEndTime(endTime)
                .serverName(serverName);

        if (!rawEventData.isNull(RECURRENCE_RULE_KEY)) {
            try {
                String rrule = parseRecurrenceRule(rawEventData.getObject(RECURRENCE_RULE_KEY));
                builder.rrule(rrule);
            } catch (Exception e) {
                log.warn("Could not parse recurrence rule for event {}: {}", eventId, e.getMessage());
            }
        }

        if (!rawEventData.isNull(GUILD_SCHEDULED_EVENT_EXCEPTIONS_KEY)) {
            DataArray exceptionsArray = rawEventData.getArray(GUILD_SCHEDULED_EVENT_EXCEPTIONS_KEY);
            List<DiscordEventExceptionDTO> exceptionDTOs = new ArrayList<>();
            for (int i = 0; i < exceptionsArray.length(); i++) {
                DataObject exData = exceptionsArray.getObject(i);
                DiscordEventExceptionDTO exDto = parseException(exData);
                if (exDto != null) {
                    exceptionDTOs.add(exDto);
                }
            }
            builder.exceptions(exceptionDTOs);
        }

        return builder.build();
    }

    protected DiscordEventExceptionDTO parseException(DataObject exData) {
        try {
            String exceptionId = exData.getString(EVENT_EXCEPTION_ID_KEY);
            String eventId = exData.getString(EVENT_ID_KEY, "");
            boolean isCancelled = exData.getBoolean(IS_CANCELED_KEY, false);

            DiscordEventExceptionDTO.DiscordEventExceptionDTOBuilder builder = DiscordEventExceptionDTO
                    .builder()
                    .exceptionId(exceptionId)
                    .eventId(eventId)
                    .isCancelled(isCancelled);

            if (!exData.isNull(RAW_SCHEDULED_START_TIME_KEY)) {
                builder.newStart(OffsetDateTime.parse(exData.getString(RAW_SCHEDULED_START_TIME_KEY)).toInstant());
            }
            if (!exData.isNull(RAW_SCHEDULED_END_TIME_KEY)) {
                builder.newEnd(OffsetDateTime.parse(exData.getString(RAW_SCHEDULED_END_TIME_KEY)).toInstant());
            }

            builder.exceptionDate(
                    net.dv8tion.jda.api.utils.TimeUtil.getTimeCreated(Long.parseLong(exceptionId)).toInstant());

            return builder.build();
        } catch (Exception e) {
            log.warn("Failed to parse event exception: {}", e.getMessage());
            return null;
        }
    }

    protected DiscordDeleteDTO parseEventDelete(DataObject data) {
        return DiscordDeleteDTO.builder()
                .id(data.getString(RAW_ID_KEY, ""))
                .guildId(data.isNull(GUILD_ID_KEY) ? null : data.getString(GUILD_ID_KEY))
                .build();
    }

    protected DiscordDeleteDTO parseExceptionDelete(DataObject data) {
        return DiscordDeleteDTO.builder()
                .id(data.getString(EVENT_EXCEPTION_ID_KEY, ""))
                .eventId(data.getString(EVENT_ID_KEY, ""))
                .guildId(data.isNull(GUILD_ID_KEY) ? null : data.getString(GUILD_ID_KEY))
                .build();
    }

    protected String parseRecurrenceRule(DataObject rule) throws InvalidRecurrenceRuleException {
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

    protected Weekday getWeekday(int discordDay) {
        return switch (discordDay) {
            case 1 -> Weekday.TU;
            case 2 -> Weekday.WE;
            case 3 -> Weekday.TH;
            case 4 -> Weekday.FR;
            case 5 -> Weekday.SA;
            case 6 -> Weekday.SU;
            default -> Weekday.MO;
        };
    }
}
