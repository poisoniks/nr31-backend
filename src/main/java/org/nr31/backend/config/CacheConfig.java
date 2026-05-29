package org.nr31.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
            cache("slotRestrictions",    Duration.ofHours(24),  100),
            cache("publishedPages",      Duration.ofHours(24),  100),
            cache("calendarEvents",      Duration.ofHours(24),  100),
            cache("fileResolution",      Duration.ofHours(24),  100),
            cache("appConfig",           Duration.ofHours(24),  100),
            cache("youtubeLatestVideo",  Duration.ofMinutes(25), 50),
            cache("discordWidget",       Duration.ofMinutes(6),  10),
            cache("youtubeTrackedChannels", Duration.ofMinutes(2), 1),
            cache("discordTrackedInvites",  Duration.ofMinutes(2), 1)
        ));
        return cacheManager;
    }

    private CaffeineCache cache(String name, Duration ttl, long maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttl)
                .recordStats()
                .build());
    }
}
