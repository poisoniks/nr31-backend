package org.nr31.backend.service;

import org.nr31.backend.dto.DiscordWidgetDataDto;

import java.util.Optional;
import java.util.Set;

/**
 * Service for fetching and caching Discord server widget data.
 * Data is fetched from Discord's public invite and widget APIs and stored
 * in the Spring cache ({@code discordWidget}) keyed by invite code.
 */
public interface DiscordWidgetService {

    /**
     * Returns the cached widget data for the given invite code.
     * On a cache miss the data is fetched on-demand and the result is cached.
     *
     * @param inviteCode Discord invite code (e.g. {@code uuc})
     * @return the widget data, or empty if the server is unreachable
     */
    Optional<DiscordWidgetDataDto> getWidgetData(String inviteCode);

    /**
     * Evicts the cached data for the given invite code so the next call to
     * {@link #getWidgetData} fetches fresh data.
     * Called by the scheduled job every 5 minutes.
     *
     * @param inviteCode Discord invite code to evict
     */
    void evictCache(String inviteCode);

    /**
     * Returns all invite codes currently referenced by Discord widgets across
     * all published and draft page revisions.
     *
     * @return set of invite codes to track
     */
    Set<String> getTrackedInviteCodes();
}
