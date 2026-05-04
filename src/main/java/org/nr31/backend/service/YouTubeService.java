package org.nr31.backend.service;

import org.nr31.backend.dto.YoutubeVideoDto;

import java.util.Optional;
import java.util.Set;

/**
 * Service for fetching and caching the latest YouTube videos per channel.
 * Videos are fetched directly from the YouTube Atom feed and stored in the
 * Spring cache ({@code youtubeLatestVideo}) keyed by channel ID.
 */
public interface YouTubeService {

    /**
     * Returns the cached latest video for the given channel ID.
     * On a cache miss the feed is fetched on-demand and the result is cached.
     *
     * @param channelId YouTube channel ID (e.g. {@code UCbU41G2hhiwdn-gFFRqZN4w})
     * @return the latest video, or empty if the channel has no videos or is unreachable
     */
    Optional<YoutubeVideoDto> getLatestVideo(String channelId);

    /**
     * Evicts the cached video for the given channel so the next call to
     * {@link #getLatestVideo} fetches fresh data.
     * Called by the scheduled job every 20 minutes.
     *
     * @param channelId YouTube channel ID to evict
     */
    void evictCache(String channelId);

    /**
     * Returns all channel IDs currently referenced by YouTube widgets across
     * all published and draft page revisions.
     *
     * @return set of channel IDs to track
     */
    Set<String> getTrackedChannelIds();
}
