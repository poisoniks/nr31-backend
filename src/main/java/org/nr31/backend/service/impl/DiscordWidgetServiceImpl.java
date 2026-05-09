package org.nr31.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.dto.DiscordMemberDto;
import org.nr31.backend.dto.DiscordWidgetDataDto;
import org.nr31.backend.model.PageRevision;
import org.nr31.backend.model.RevisionStatus;
import org.nr31.backend.repository.PageRevisionRepository;
import org.nr31.backend.service.DiscordWidgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DiscordWidgetServiceImpl implements DiscordWidgetService {

    private final PageRevisionRepository pageRevisionRepository;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private static final String INVITE_API_URL = "https://discord.com/api/invites/";
    private static final String WIDGET_API_URL = "https://discord.com/api/guilds/%s/widget.json";
    private static final String ICON_CDN_URL = "https://cdn.discordapp.com/icons/%s/%s.png";

    @Autowired
    public DiscordWidgetServiceImpl(PageRevisionRepository pageRevisionRepository, ObjectMapper objectMapper) {
        this(pageRevisionRepository, objectMapper, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    public DiscordWidgetServiceImpl(PageRevisionRepository pageRevisionRepository, ObjectMapper objectMapper, HttpClient httpClient) {
        this.pageRevisionRepository = pageRevisionRepository;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    @Cacheable(value = "discordWidget", key = "#inviteCode")
    public Optional<DiscordWidgetDataDto> getWidgetData(String inviteCode) {
        log.info("Cache miss for Discord invite {} — fetching on-demand", inviteCode);
        return fetchWidgetDataFromApi(inviteCode);
    }

    @Override
    @CacheEvict(value = "discordWidget", key = "#inviteCode")
    public void evictCache(String inviteCode) {
        log.debug("Evicted Discord cache for invite {}", inviteCode);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getTrackedInviteCodes() {
        Set<String> inviteCodes = new HashSet<>();

        List<PageRevision> revisions = pageRevisionRepository.findAllByStatus(RevisionStatus.PUBLISHED);
        revisions.forEach(r -> extractInviteCodes(r.getLayoutData(), inviteCodes));

        List<PageRevision> drafts = pageRevisionRepository.findAllByStatus(RevisionStatus.DRAFT);
        drafts.forEach(r -> extractInviteCodes(r.getLayoutData(), inviteCodes));

        return inviteCodes;
    }

    private Optional<DiscordWidgetDataDto> fetchWidgetDataFromApi(String inviteCode) {
        try {
            // 1. Get Guild ID and Icon from Invite API
            HttpRequest inviteRequest = HttpRequest.newBuilder()
                    .uri(URI.create(INVITE_API_URL + inviteCode))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> inviteResponse = httpClient.send(inviteRequest, HttpResponse.BodyHandlers.ofString());
            if (inviteResponse.statusCode() != 200) {
                log.warn("Discord invite API returned HTTP {} for code {}", inviteResponse.statusCode(), inviteCode);
                return Optional.empty();
            }

            JsonNode inviteJson = objectMapper.readTree(inviteResponse.body());
            JsonNode guildNode = inviteJson.path("guild");
            String guildId = guildNode.path("id").asText("");
            String iconHash = guildNode.path("icon").asText("");

            if (guildId.isEmpty()) {
                log.warn("Could not find guild ID for invite code {}", inviteCode);
                return Optional.empty();
            }

            String logoUrl = iconHash.isEmpty() ? null : String.format(ICON_CDN_URL, guildId, iconHash);

            // 2. Get Widget Data using Guild ID
            HttpRequest widgetRequest = HttpRequest.newBuilder()
                    .uri(URI.create(String.format(WIDGET_API_URL, guildId)))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> widgetResponse = httpClient.send(widgetRequest, HttpResponse.BodyHandlers.ofString());
            if (widgetResponse.statusCode() != 200) {
                log.warn("Discord widget API returned HTTP {} for guild {}", widgetResponse.statusCode(), guildId);
                return Optional.empty();
            }

            JsonNode widgetJson = objectMapper.readTree(widgetResponse.body());
            String serverName = widgetJson.path("name").asText("");
            int presenceCount = widgetJson.path("presence_count").asInt(0);

            // 3. Process Members and Games
            List<DiscordMemberDto> members = new ArrayList<>();
            Map<String, Integer> gameCounts = new HashMap<>();

            JsonNode membersNode = widgetJson.path("members");
            if (membersNode.isArray()) {
                for (JsonNode m : membersNode) {
                    String gameName = m.path("game").path("name").asText(null);
                    if (gameName != null) {
                        gameCounts.put(gameName, gameCounts.getOrDefault(gameName, 0) + 1);
                    }

                    members.add(DiscordMemberDto.builder()
                            .id(m.path("id").asText(""))
                            .username(m.path("username").asText(""))
                            .avatarUrl(m.path("avatar_url").asText(""))
                            .status(m.path("status").asText("online"))
                            .gameName(gameName)
                            .build());
                }
            }

            // Top 3 games
            List<String> topGames = gameCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            // Sorted and sliced members (15 max)
            // Sorting logic: playing game (score 2) + in channel (score 1)
            List<DiscordMemberDto> displayMembers = widgetJson.path("members").isArray() ? 
                java.util.stream.StreamSupport.stream(widgetJson.path("members").spliterator(), false)
                .sorted((a, b) -> {
                    int aScore = (a.has("game") ? 2 : 0) + (a.has("channel_id") ? 1 : 0);
                    int bScore = (b.has("game") ? 2 : 0) + (b.has("channel_id") ? 1 : 0);
                    return bScore - aScore;
                })
                .limit(15)
                .map(m -> DiscordMemberDto.builder()
                        .id(m.path("id").asText(""))
                        .username(m.path("username").asText(""))
                        .avatarUrl(m.path("avatar_url").asText(""))
                        .status(m.path("status").asText("online"))
                        .gameName(m.path("game").path("name").asText(null))
                        .build())
                .collect(Collectors.toList()) : Collections.emptyList();

            int moreCount = Math.max(0, presenceCount - displayMembers.size());

            return Optional.of(DiscordWidgetDataDto.builder()
                    .serverName(serverName)
                    .logoUrl(logoUrl)
                    .presenceCount(presenceCount)
                    .topGames(topGames)
                    .displayMembers(displayMembers)
                    .moreCount(moreCount)
                    .inviteUrl("https://discord.com/invite/" + inviteCode)
                    .build());

        } catch (Exception e) {
            log.error("Error fetching Discord widget data for invite {}: {}", inviteCode, e.getMessage(), e);
            return Optional.empty();
        }
    }

    private void extractInviteCodes(JsonNode layoutData, Set<String> inviteCodes) {
        if (layoutData == null) {
            return;
        }
        JsonNode slots = layoutData.path("slots");
        if (!slots.isArray()) {
            return;
        }
        for (JsonNode slot : slots) {
            JsonNode widgets = slot.path("widgets");
            if (!widgets.isArray()) {
                continue;
            }
            for (JsonNode widget : widgets) {
                if ("discord".equals(widget.path("type").asText(""))) {
                    String code = widget.path("inviteCode").asText("");
                    if (!code.isEmpty()) {
                        inviteCodes.add(code);
                    }
                }
            }
        }
    }
}
