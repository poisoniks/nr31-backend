package org.nr31.backend.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.service.DiscordWidgetService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class DiscordWidgetFetchJob {

    private final DiscordWidgetService discordWidgetService;

    /**
     * Runs every 5 minutes (with a 5-seconds initial delay to let the app boot).
     * Evicts the stale cache entry for each tracked invite code and immediately
     * re-warms it via {@link DiscordWidgetService#getWidgetData} so the cache is
     * never cold for active widgets.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 5000)
    public void fetchLatestDiscordData() {
        log.info("Starting scheduled Discord widget data fetch...");

        Set<String> inviteCodes = discordWidgetService.getTrackedInviteCodes();

        if (inviteCodes.isEmpty()) {
            log.info("No Discord widgets to track, skipping fetch.");
            return;
        }

        log.info("Refreshing Discord cache for {} invite code(s): {}", inviteCodes.size(), inviteCodes);

        for (String inviteCode : inviteCodes) {
            discordWidgetService.evictCache(inviteCode);
            discordWidgetService.getWidgetData(inviteCode);
        }

        log.info("Scheduled Discord widget data fetch finished.");
    }
}
