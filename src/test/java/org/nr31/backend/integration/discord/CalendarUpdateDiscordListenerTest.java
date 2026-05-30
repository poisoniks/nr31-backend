package org.nr31.backend.integration.discord;

import tools.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.RawGatewayEvent;
import net.dv8tion.jda.api.utils.TimeUtil;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nr31.backend.dto.integration.DiscordSyncEventDTO;
import org.nr31.backend.dto.integration.DiscordSyncExceptionDTO;
import org.nr31.backend.service.AppConfigService;
import org.nr31.backend.service.CalendarService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarUpdateDiscordListenerTest {

    @Mock
    private CalendarService calendarService;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private AppConfigService appConfigService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private JDA jda;
    @Mock
    private Guild guild;
    @Mock
    private Cache cache;

    @InjectMocks
    private CalendarUpdateDiscordListener listener;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        lenient().when(cacheManager.getCache(CalendarUpdateDiscordListener.CALENDAR_CACHE_NAME)).thenReturn(cache);
    }

    @Test
    void shouldCorrectlyParseRawGatewayEvent() {
        DataObject payload = DataObject.empty()
                .put("id", "123")
                .put("guild_id", "456")
                .put("name", "Gateway Event")
                .put("scheduled_start_time", "2026-03-20T10:00:00Z");

        RawGatewayEvent rawEvent = mock(RawGatewayEvent.class);
        when(rawEvent.getType()).thenReturn("GUILD_SCHEDULED_EVENT_CREATE");
        when(rawEvent.getPayload()).thenReturn(payload);
        when(rawEvent.getJDA()).thenReturn(jda);
        when(jda.getGuildById("456")).thenReturn(guild);
        when(guild.getName()).thenReturn("Test Server");

        listener.onRawGateway(rawEvent);

        ArgumentCaptor<DiscordSyncEventDTO> captor = ArgumentCaptor.forClass(DiscordSyncEventDTO.class);
        verify(calendarService).syncDiscordEvent(captor.capture());
        
        DiscordSyncEventDTO syncDto = captor.getValue();
        assertEquals("123", syncDto.getDiscordId());
        assertEquals("Gateway Event", syncDto.getName());
        assertEquals("Test Server", syncDto.getServerName());
    }

    @Test
    void shouldParseComplexRecurrenceRule() {
        DataObject rruleObj = DataObject.empty()
                .put("frequency", 2) // WEEKLY
                .put("interval", 2)
                .put("by_weekday", DataArray.fromCollection(List.of(1, 3))); // TU, TH

        DataObject payload = DataObject.empty()
                .put("id", "123")
                .put("name", "Recurring Event")
                .put("scheduled_start_time", "2026-03-20T10:00:00Z")
                .put("recurrence_rule", rruleObj);

        RawGatewayEvent rawEvent = mock(RawGatewayEvent.class);
        when(rawEvent.getType()).thenReturn("GUILD_SCHEDULED_EVENT_UPDATE");
        when(rawEvent.getPayload()).thenReturn(payload);

        listener.onRawGateway(rawEvent);

        ArgumentCaptor<DiscordSyncEventDTO> captor = ArgumentCaptor.forClass(DiscordSyncEventDTO.class);
        verify(calendarService).syncDiscordEvent(captor.capture());
        
        String rrule = captor.getValue().getRrule();
        assertNotNull(rrule);
        assertTrue(rrule.contains("FREQ=WEEKLY"));
        assertTrue(rrule.contains("INTERVAL=2"));
        assertTrue(rrule.contains("BYDAY=TU,TH"));
    }

    @Test
    void shouldParseEventExceptionsInPayload() {
        long snowflake = TimeUtil.getDiscordTimestamp(Instant.parse("2026-03-19T10:00:00Z").toEpochMilli());
        DataObject exceptionObj = DataObject.empty()
                .put("event_exception_id", String.valueOf(snowflake))
                .put("event_id", "123")
                .put("is_canceled", true);

        DataObject payload = DataObject.empty()
                .put("id", "123")
                .put("name", "Event with Exceptions")
                .put("scheduled_start_time", "2026-03-20T10:00:00Z")
                .put("guild_scheduled_event_exceptions", DataArray.fromCollection(List.of(exceptionObj)));

        RawGatewayEvent rawEvent = mock(RawGatewayEvent.class);
        when(rawEvent.getType()).thenReturn("GUILD_SCHEDULED_EVENT_CREATE");
        when(rawEvent.getPayload()).thenReturn(payload);

        listener.onRawGateway(rawEvent);

        ArgumentCaptor<DiscordSyncEventDTO> captor = ArgumentCaptor.forClass(DiscordSyncEventDTO.class);
        verify(calendarService).syncDiscordEvent(captor.capture());
        
        List<DiscordSyncExceptionDTO> exceptions = captor.getValue().getExceptions();
        assertEquals(1, exceptions.size());
        assertEquals(String.valueOf(snowflake), exceptions.get(0).getExceptionId());
        assertTrue(exceptions.get(0).isCancelled());
    }

    @Test
    void shouldHandleScheduleEventDeleted() {
        DataObject payload = DataObject.empty().put("id", "ev_to_delete");
        RawGatewayEvent rawEvent = mock(RawGatewayEvent.class);
        when(rawEvent.getType()).thenReturn("GUILD_SCHEDULED_EVENT_DELETE");
        when(rawEvent.getPayload()).thenReturn(payload);

        listener.onRawGateway(rawEvent);

        verify(calendarService).deleteDiscordEvent("ev_to_delete");
        verify(cache).clear();
    }

    @Test
    void shouldHandleExceptionCreatedThroughGateway() {
        long snowflake = TimeUtil.getDiscordTimestamp(Instant.parse("2026-03-19T10:00:00Z").toEpochMilli());
        DataObject payload = DataObject.empty()
                .put("event_exception_id", String.valueOf(snowflake))
                .put("event_id", "parent_ev")
                .put("is_canceled", true);

        RawGatewayEvent rawEvent = mock(RawGatewayEvent.class);
        when(rawEvent.getType()).thenReturn("GUILD_SCHEDULED_EVENT_EXCEPTION_CREATE");
        when(rawEvent.getPayload()).thenReturn(payload);

        listener.onRawGateway(rawEvent);

        ArgumentCaptor<DiscordSyncExceptionDTO> captor = ArgumentCaptor.forClass(DiscordSyncExceptionDTO.class);
        verify(calendarService).syncDiscordEventException(eq("parent_ev"), captor.capture());

        assertEquals(String.valueOf(snowflake), captor.getValue().getExceptionId());
        assertTrue(captor.getValue().isCancelled());
        verify(cache).clear();
    }

    @Test
    void shouldHandleExceptionDeletedThroughGateway() {
        DataObject payload = DataObject.empty()
                .put("event_exception_id", "ex_to_delete")
                .put("event_id", "parent_ev");

        RawGatewayEvent rawEvent = mock(RawGatewayEvent.class);
        when(rawEvent.getType()).thenReturn("GUILD_SCHEDULED_EVENT_EXCEPTION_DELETE");
        when(rawEvent.getPayload()).thenReturn(payload);

        listener.onRawGateway(rawEvent);

        verify(calendarService).deleteDiscordEventException("ex_to_delete");
        verify(cache).clear();
    }
}
