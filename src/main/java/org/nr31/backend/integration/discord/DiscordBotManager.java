package org.nr31.backend.integration.discord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordBotManager {

    @Value("${app.discord.bot-token}")
    private String botToken;

    private final CalendarUpdateDiscordListener calendarUpdateDiscordListener;

    private JDA jdaInstance;

    @EventListener(ApplicationReadyEvent.class)
    public void autoStartOnBoot() {
        log.info("Spring Boot started. Attempting to start Discord Bot...");
        startBot();
    }

    public synchronized void startBot() {
        if (jdaInstance != null) {
            log.warn("Discord Bot is already running or initializing.");
            return;
        }

        try {
            jdaInstance = JDABuilder.createLight(botToken, GatewayIntent.SCHEDULED_EVENTS)
                    .enableCache(CacheFlag.SCHEDULED_EVENTS)
                    .addEventListeners(calendarUpdateDiscordListener)
                    .setEventPassthrough(true)
                    .setRawEventsEnabled(true)
                    .build();
            log.info("Discord Bot connection initiated.");
        } catch (Exception e) {
            log.error("Failed to start Discord Bot. Token might be invalid or network is down.", e);
            jdaInstance = null;
        }
    }

    public synchronized void stopBot() {
        if (jdaInstance != null) {
            log.info("Shutting down Discord Bot...");
            jdaInstance.shutdown();
            jdaInstance = null;
            log.info("Discord Bot stopped.");
        } else {
            log.warn("Discord Bot is not running.");
        }
    }

    public synchronized String getBotStatus() {
        if (jdaInstance == null) {
            return "OFFLINE";
        }
        return jdaInstance.getStatus().toString();
    }
}
