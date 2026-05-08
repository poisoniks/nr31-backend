package org.nr31.backend.integration.discord;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.requests.Route;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import org.nr31.backend.dto.AppConfigDto;
import org.nr31.backend.dto.DiscordSyncEventDTO;
import org.nr31.backend.dto.DiscordSyncExceptionDTO;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.integration.discord.dto.DiscordDeleteDTO;
import org.nr31.backend.integration.discord.dto.DiscordEventExceptionDTO;
import org.nr31.backend.integration.discord.dto.DiscordScheduledEventDTO;
import org.nr31.backend.service.AppConfigService;
import org.nr31.backend.service.CalendarService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CalendarUpdateDiscordListener extends UpdatedListenerAdapter {
    protected static final String CALENDAR_CACHE_NAME = "calendarEvents";
    protected static final String SCHEDULED_EVENTS_ROUTE = "guilds/{guild_id}/scheduled-events";
    public static final String GUILD_ID_PROPERTY = "fetch_scheduled_discord_events_guild_id";
    public static final String GUILD_ID_PROPERTY_KEY = "guildId";

    private final CalendarService calendarService;
    private final CacheManager cacheManager;
    private final TransactionTemplate transactionTemplate;
    private final AppConfigService appConfigService;
    private final ObjectMapper objectMapper;

    @Override
    public void onReady(ReadyEvent event) {
        log.info("Discord bot is ready. Running initial event sync...");
        String guildId = getGuildId();
        if (guildId == null || guildId.isBlank()) {
            log.info("Discord guild ID is not configured (or empty). Skipping initial event sync.");
            return;
        }

        try {
            event.getJDA().getGuilds().stream()
                    .filter(g -> g.getId().equals(guildId))
                    .findFirst()
                    .ifPresent(guild -> syncAllGuildEvents(event.getJDA(), guild.getId()));
            log.info("Events sync finished successfully.");
        } catch (Exception e) {
            log.error("Failed to sync Discord events on startup", e);
        }
    }

    private void syncAllGuildEvents(JDA jda, String guildId) {
        Route.CompiledRoute route = Route.get(SCHEDULED_EVENTS_ROUTE).compile(guildId);
        new RestActionImpl<DataArray>(jda, route, (response, request) -> response.getArray())
                .queue(rawEvents -> {
                    transactionTemplate.executeWithoutResult(s -> {
                        List<String> syncedServerEventIds = new ArrayList<>();
                        for (int i = 0; i < rawEvents.length(); i++) {
                            try {
                                DataObject rawEvent = rawEvents.getObject(i);
                                DiscordScheduledEventDTO rawDto = parseEvent(rawEvent, jda);
                                syncEventFromRawDTO(rawDto);
                                syncedServerEventIds.add(rawDto.getId());
                            } catch (Exception e) {
                                log.warn("Failed to sync scheduled event from raw data during syncAllGuildEvents", e);
                            }
                        }
                        calendarService.removeOrphanedDiscordEvents(syncedServerEventIds);
                        evictCache();
                    });
                }, e -> log.error("Failed to fetch scheduled events for guild {}", guildId, e));
    }

    @Override
    protected void onScheduleEventCreated(DiscordScheduledEventDTO event) {
        log.info("Processing Discord Scheduled Event (Create): {}", event.getName());
        transactionTemplate.executeWithoutResult(s -> {
            syncEventFromRawDTO(event);
            evictCache();
        });
    }

    @Override
    protected void onScheduleEventUpdated(DiscordScheduledEventDTO event) {
        log.info("Processing Discord Scheduled Event (Update): {}", event.getName());
        transactionTemplate.executeWithoutResult(s -> {
            syncEventFromRawDTO(event);
            evictCache();
        });
    }

    @Override
    protected void onScheduleEventDeleted(DiscordDeleteDTO event) {
        log.info("Processing Discord Scheduled Event (Delete)");
        transactionTemplate.executeWithoutResult(s -> {
            calendarService.deleteDiscordEvent(event.getId());
            evictCache();
        });
    }

    @Override
    protected void onScheduleEventExceptionCreated(DiscordEventExceptionDTO exception) {
        log.info("Processing Discord Scheduled Event Exception (Create/Update)");
        transactionTemplate.executeWithoutResult(s -> {
            if (exception != null && exception.getEventId() != null && !exception.getEventId().isEmpty()) {
                calendarService.syncDiscordEventException(exception.getEventId(), mapExceptionDTO(exception));
            }
            evictCache();
        });
    }

    @Override
    protected void onScheduleEventExceptionUpdated(DiscordEventExceptionDTO exception) {
        onScheduleEventExceptionCreated(exception);
    }

    @Override
    protected void onScheduleEventExceptionDeleted(DiscordDeleteDTO exception) {
        log.info("Processing Discord Scheduled Event Exception (Raw Delete)");
        transactionTemplate.executeWithoutResult(s -> {
            if (exception != null && exception.getId() != null && !exception.getId().isEmpty()) {
                calendarService.deleteDiscordEventException(exception.getId());
            }
            evictCache();
        });
    }

    private void syncEventFromRawDTO(DiscordScheduledEventDTO rawDto) {
        DiscordSyncEventDTO.DiscordSyncEventDTOBuilder dtoBuilder = DiscordSyncEventDTO.builder()
                .discordId(rawDto.getId())
                .name(rawDto.getName())
                .description(rawDto.getDescription())
                .start(rawDto.getScheduledStartTime())
                .end(rawDto.getScheduledEndTime())
                .serverName(rawDto.getServerName())
                .rrule(rawDto.getRrule())
                .timezone(rawDto.getTimezone());

        if (rawDto.getExceptions() != null) {
            List<DiscordSyncExceptionDTO> mappedExceptions = rawDto.getExceptions().stream()
                    .map(this::mapExceptionDTO)
                    .collect(Collectors.toList());
            dtoBuilder.exceptions(mappedExceptions);
        }

        calendarService.syncDiscordEvent(dtoBuilder.build());
    }

    private DiscordSyncExceptionDTO mapExceptionDTO(DiscordEventExceptionDTO rawEx) {
        return DiscordSyncExceptionDTO.builder()
                .exceptionId(rawEx.getExceptionId())
                .isCancelled(rawEx.isCancelled())
                .newStart(rawEx.getNewStart())
                .newEnd(rawEx.getNewEnd())
                .exceptionDate(rawEx.getExceptionDate() != null ? rawEx.getExceptionDate() : Instant.now())
                .build();
    }

    private void evictCache() {
        Cache cache = cacheManager.getCache(CALENDAR_CACHE_NAME);
        if (cache != null) {
            cache.clear();
        }
    }

    private String getGuildId() {
        try {
            AppConfigDto config = appConfigService.getConfig(GUILD_ID_PROPERTY);
            JsonNode configNode = objectMapper.readTree(config.getConfigValue());
            JsonNode valueNode = configNode.get(GUILD_ID_PROPERTY_KEY);
            if (valueNode != null && !valueNode.isNull()) {
                return valueNode.asString();
            }
            return null;
        } catch (ElementNotFoundException e) {
            log.warn("Guild Id to fetch scheduled Discord events is not found");
            return null;
        } catch (Exception e) {
            log.error("Failed to parse '{}' configuration. Returning null.", GUILD_ID_PROPERTY, e);
            return null;
        }
    }
}
