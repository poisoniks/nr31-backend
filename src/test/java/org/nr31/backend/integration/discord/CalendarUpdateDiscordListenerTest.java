package org.nr31.backend.integration.discord;

import net.dv8tion.jda.api.entities.ScheduledEvent;
import net.dv8tion.jda.api.events.guild.scheduledevent.ScheduledEventCreateEvent;
import net.dv8tion.jda.api.events.guild.scheduledevent.ScheduledEventDeleteEvent;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nr31.backend.dto.DiscordSyncEventDTO;
import org.nr31.backend.dto.DiscordSyncExceptionDTO;
import org.nr31.backend.service.CalendarService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.nr31.backend.integration.discord.CalendarUpdateDiscordListener.BY_WEEKDAY_KEY;
import static org.nr31.backend.integration.discord.CalendarUpdateDiscordListener.CALENDAR_CACHE_NAME;
import static org.nr31.backend.integration.discord.CalendarUpdateDiscordListener.EVENT_EXCEPTION_ID_KEY;
import static org.nr31.backend.integration.discord.CalendarUpdateDiscordListener.FREQUENCY_KEY;
import static org.nr31.backend.integration.discord.CalendarUpdateDiscordListener.GUILD_SCHEDULED_EVENT_EXCEPTIONS_KEY;
import static org.nr31.backend.integration.discord.CalendarUpdateDiscordListener.INTERVAL_KEY;
import static org.nr31.backend.integration.discord.CalendarUpdateDiscordListener.IS_CANCELED_KEY;
import static org.nr31.backend.integration.discord.CalendarUpdateDiscordListener.RAW_SCHEDULED_END_TIME_KEY;
import static org.nr31.backend.integration.discord.CalendarUpdateDiscordListener.RAW_SCHEDULED_START_TIME_KEY;
import static org.nr31.backend.integration.discord.CalendarUpdateDiscordListener.RECURRENCE_RULE_KEY;

@ExtendWith(MockitoExtension.class)
class CalendarUpdateDiscordListenerTest {

    private static final String MOCK_EVENT_ID = "event123";
    private static final String MOCK_EVENT_NAME = "Test Event";
    private static final String MOCK_START_TIME = "2026-03-15T12:00:00Z";
    private static final String MOCK_END_TIME = "2026-03-15T14:00:00Z";
    private static final String MOCK_LOCATION = "Voice Channel";
    private static final String MOCK_DESCRIPTION = "Fun times";
    private static final String MOCK_EXCEPTION_ID = "ex123";
    private static final String MOCK_EXCEPTION_TIME = "2026-03-20T18:00:00Z";

    @Mock
    private CalendarService calendarService;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private CalendarUpdateDiscordListener calendarUpdateDiscordListener;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void shouldSyncEventsOnApplicationStart() {
        ScheduledEventCreateEvent event = mock(ScheduledEventCreateEvent.class);
        ScheduledEvent discordEvent = mock(ScheduledEvent.class);
        
        DataObject rruleObj = DataObject.empty()
                .put(FREQUENCY_KEY, 2)
                .put(INTERVAL_KEY, 1)
                .put(BY_WEEKDAY_KEY, DataArray.fromCollection(List.of(3)));

        DataObject rawData = DataObject.empty()
                .put("d", DataObject.empty()
                        .put(RECURRENCE_RULE_KEY, rruleObj));

        when(event.getScheduledEvent()).thenReturn(discordEvent);
        when(event.getRawData()).thenReturn(rawData);
        when(discordEvent.getId()).thenReturn(MOCK_EVENT_ID);
        when(discordEvent.getName()).thenReturn(MOCK_EVENT_NAME);
        when(discordEvent.getStartTime()).thenReturn(OffsetDateTime.parse(MOCK_START_TIME));
        lenient().when(discordEvent.getGuild()).thenReturn(mock(net.dv8tion.jda.api.entities.Guild.class));
        lenient().when(discordEvent.getGuild().getName()).thenReturn("Test Server");

        calendarUpdateDiscordListener.onGenericScheduledEventGateway(event);

        ArgumentCaptor<DiscordSyncEventDTO> captor = ArgumentCaptor.forClass(DiscordSyncEventDTO.class);
        verify(calendarService, atLeastOnce()).syncDiscordEvent(captor.capture());
        
        DiscordSyncEventDTO saved = captor.getValue();
        assertNotNull(saved.getRrule());
        assertTrue(saved.getRrule().contains("FREQ=WEEKLY"));
        assertTrue(saved.getRrule().contains("BYDAY=TH"));
    }

    @Test
    void shouldRemoveOrphanedFutureEvents() {
        ScheduledEventCreateEvent event = mock(ScheduledEventCreateEvent.class);
        ScheduledEvent discordEvent = mock(ScheduledEvent.class);

        DataObject rawData = DataObject.empty()
                .put("d", DataObject.empty()
                        .put(RECURRENCE_RULE_KEY, null));

        when(event.getScheduledEvent()).thenReturn(discordEvent);
        when(event.getRawData()).thenReturn(rawData);
        when(discordEvent.getId()).thenReturn(MOCK_EVENT_ID);
        when(discordEvent.getName()).thenReturn(MOCK_EVENT_NAME);
        when(discordEvent.getStartTime()).thenReturn(OffsetDateTime.parse(MOCK_START_TIME));
        lenient().when(discordEvent.getGuild()).thenReturn(mock(net.dv8tion.jda.api.entities.Guild.class));
        lenient().when(discordEvent.getGuild().getName()).thenReturn("Test Server");

        calendarUpdateDiscordListener.onGenericScheduledEventGateway(event);

        verify(calendarService, atLeastOnce()).syncDiscordEvent(any(DiscordSyncEventDTO.class));
    }

    @Test
    void shouldHandleGenericScheduledEvents() {
        ScheduledEventCreateEvent event = mock(ScheduledEventCreateEvent.class);
        ScheduledEvent discordEvent = mock(ScheduledEvent.class);
        
        DataObject rawData = DataObject.empty()
                .put("d", DataObject.empty()
                        .put(RECURRENCE_RULE_KEY, null));

        when(event.getScheduledEvent()).thenReturn(discordEvent);
        when(event.getRawData()).thenReturn(rawData);
        when(discordEvent.getId()).thenReturn(MOCK_EVENT_ID);
        when(discordEvent.getName()).thenReturn(MOCK_EVENT_NAME);
        when(discordEvent.getDescription()).thenReturn(MOCK_DESCRIPTION);
        when(discordEvent.getStartTime()).thenReturn(OffsetDateTime.parse(MOCK_START_TIME));
        when(discordEvent.getEndTime()).thenReturn(OffsetDateTime.parse(MOCK_END_TIME));
        lenient().when(discordEvent.getGuild()).thenReturn(mock(net.dv8tion.jda.api.entities.Guild.class));
        when(discordEvent.getGuild().getName()).thenReturn(MOCK_LOCATION);

        calendarUpdateDiscordListener.onGenericScheduledEventGateway(event);

        ArgumentCaptor<DiscordSyncEventDTO> captor = ArgumentCaptor.forClass(DiscordSyncEventDTO.class);
        verify(calendarService, atLeastOnce()).syncDiscordEvent(captor.capture());
        
        DiscordSyncEventDTO saved = captor.getValue();
        assertEquals(MOCK_EVENT_ID, saved.getDiscordId());
        assertEquals(MOCK_EVENT_NAME, saved.getName());
        assertEquals(MOCK_DESCRIPTION, saved.getDescription());
        assertEquals(MOCK_LOCATION, saved.getServerName());
        assertNull(saved.getRrule());
    }

    @Test
    void shouldUseRecursiveRuleOnEventCreation() {
        ScheduledEventCreateEvent event = mock(ScheduledEventCreateEvent.class);
        ScheduledEvent discordEvent = mock(ScheduledEvent.class);
        
        DataObject rruleObj = DataObject.empty()
                .put(FREQUENCY_KEY, 2)
                .put(INTERVAL_KEY, 1)
                .put(BY_WEEKDAY_KEY, DataArray.fromCollection(List.of(0, 2, 4)));

        DataObject rawData = DataObject.empty()
                .put("d", DataObject.empty()
                        .put(RECURRENCE_RULE_KEY, rruleObj));

        when(event.getScheduledEvent()).thenReturn(discordEvent);
        when(event.getRawData()).thenReturn(rawData);
        when(discordEvent.getId()).thenReturn(MOCK_EVENT_ID);
        when(discordEvent.getName()).thenReturn(MOCK_EVENT_NAME);
        when(discordEvent.getStartTime()).thenReturn(OffsetDateTime.parse(MOCK_START_TIME));
        lenient().when(discordEvent.getGuild()).thenReturn(mock(net.dv8tion.jda.api.entities.Guild.class));
        lenient().when(discordEvent.getGuild().getName()).thenReturn("Test Server");

        calendarUpdateDiscordListener.onGenericScheduledEventGateway(event);

        ArgumentCaptor<DiscordSyncEventDTO> captor = ArgumentCaptor.forClass(DiscordSyncEventDTO.class);
        verify(calendarService, atLeastOnce()).syncDiscordEvent(captor.capture());
        
        DiscordSyncEventDTO saved = captor.getValue();
        assertNotNull(saved.getRrule());
        assertTrue(saved.getRrule().contains("FREQ=WEEKLY"));
        assertTrue(saved.getRrule().contains("BYDAY=MO,WE,FR"));
    }

    @Test
    void shouldCreateExceptionsOnUpdateOfSeriesInstance() {
        ScheduledEventCreateEvent event = mock(ScheduledEventCreateEvent.class);
        ScheduledEvent discordEvent = mock(ScheduledEvent.class);
        
        DataObject exceptionObj = DataObject.empty()
                .put(EVENT_EXCEPTION_ID_KEY, MOCK_EXCEPTION_ID)
                .put(IS_CANCELED_KEY, true)
                .put(RAW_SCHEDULED_START_TIME_KEY, MOCK_EXCEPTION_TIME)
                .put(RAW_SCHEDULED_END_TIME_KEY, MOCK_EXCEPTION_TIME);

        DataObject rawData = DataObject.empty()
                .put("d", DataObject.empty()
                        .put(RECURRENCE_RULE_KEY, null)
                        .put(GUILD_SCHEDULED_EVENT_EXCEPTIONS_KEY, DataArray.fromCollection(List.of(exceptionObj))));

        when(event.getScheduledEvent()).thenReturn(discordEvent);
        when(event.getRawData()).thenReturn(rawData);
        when(discordEvent.getId()).thenReturn(MOCK_EVENT_ID);
        when(discordEvent.getName()).thenReturn(MOCK_EVENT_NAME);
        when(discordEvent.getStartTime()).thenReturn(OffsetDateTime.parse(MOCK_START_TIME));
        lenient().when(discordEvent.getGuild()).thenReturn(mock(net.dv8tion.jda.api.entities.Guild.class));
        lenient().when(discordEvent.getGuild().getName()).thenReturn("Test Server");

        calendarUpdateDiscordListener.onGenericScheduledEventGateway(event);

        ArgumentCaptor<DiscordSyncEventDTO> rootCaptor = ArgumentCaptor.forClass(DiscordSyncEventDTO.class);
        verify(calendarService, atLeastOnce()).syncDiscordEvent(rootCaptor.capture());
        
        DiscordSyncEventDTO savedRoot = rootCaptor.getValue();
        assertEquals(1, savedRoot.getExceptions().size());

        DiscordSyncExceptionDTO savedEx = savedRoot.getExceptions().get(0);
        assertEquals(MOCK_EXCEPTION_ID, savedEx.getExceptionId());
        assertTrue(savedEx.isCancelled());
        assertEquals(OffsetDateTime.parse(MOCK_EXCEPTION_TIME).toInstant(), savedEx.getNewStart());
        assertEquals(OffsetDateTime.parse(MOCK_EXCEPTION_TIME).toInstant(), savedEx.getExceptionDate());
    }

    @Test
    void shouldDeleteEvent() {
        ScheduledEventDeleteEvent event = mock(ScheduledEventDeleteEvent.class);
        ScheduledEvent discordEvent = mock(ScheduledEvent.class);
        
        when(event.getScheduledEvent()).thenReturn(discordEvent);
        when(discordEvent.getId()).thenReturn(MOCK_EVENT_ID);

        calendarUpdateDiscordListener.onGenericScheduledEventGateway(event);

        verify(calendarService).deleteDiscordEvent(MOCK_EVENT_ID);

        Cache cache = mock(Cache.class);
        when(cacheManager.getCache(CALENDAR_CACHE_NAME)).thenReturn(cache);
        calendarUpdateDiscordListener.onGenericScheduledEventGateway(event);
        verify(cache, atLeastOnce()).clear();
    }
}
