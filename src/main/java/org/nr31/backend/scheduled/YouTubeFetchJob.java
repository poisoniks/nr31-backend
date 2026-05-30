package org.nr31.backend.scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.service.YouTubeService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class YouTubeFetchJob {

    private final YouTubeService youTubeService;

    /**
     * Runs every 20 minutes (with a 5-second initial delay to let the app boot).
     * Evicts the stale cache entry for each tracked channel and immediately
     * re-warms it via {@link YouTubeService#getLatestVideo} so the cache is
     * never cold for active channels.
     */
    @Scheduled(fixedRate = 20 * 60 * 1000, initialDelay = 5000)
    public void fetchLatestVideos() {
        log.info("Starting scheduled YouTube video fetch...");

        Set<String> channelIds = youTubeService.getTrackedChannelIds();

        if (channelIds.isEmpty()) {
            log.info("No YouTube channels to track, skipping fetch.");
            return;
        }

        log.info("Refreshing YouTube cache for {} channel(s): {}", channelIds.size(), channelIds);

        for (String channelId : channelIds) {
            youTubeService.evictCache(channelId);
            youTubeService.getLatestVideo(channelId);
        }

        log.info("Scheduled YouTube video fetch finished.");
    }
}
