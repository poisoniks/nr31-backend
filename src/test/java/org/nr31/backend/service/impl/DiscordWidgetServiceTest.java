package org.nr31.backend.service.impl;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nr31.backend.dto.DiscordWidgetDataDto;
import org.nr31.backend.repository.PageRevisionRepository;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscordWidgetServiceTest {

    @Mock
    private PageRevisionRepository pageRevisionRepository;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> inviteResponse;

    @Mock
    private HttpResponse<String> widgetResponse;

    private DiscordWidgetServiceImpl discordWidgetService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        discordWidgetService = new DiscordWidgetServiceImpl(pageRevisionRepository, objectMapper, httpClient);
    }

    @Test
    void shouldFetchWidgetDataSuccessfully() throws Exception {
        String inviteCode = "uuc";
        String guildId = "454665524400619535";
        String inviteJson = "{\"guild\": {\"id\": \"" + guildId + "\", \"icon\": \"a_hash\"}}";
        String widgetJson = "{\"name\": \"Test Server\", \"presence_count\": 100, \"members\": [" +
                "{\"id\": \"1\", \"username\": \"user1\", \"status\": \"online\", \"game\": {\"name\": \"Game1\"}}," +
                "{\"id\": \"2\", \"username\": \"user2\", \"status\": \"online\"}" +
                "]}";

        when(inviteResponse.statusCode()).thenReturn(200);
        when(inviteResponse.body()).thenReturn(inviteJson);
        when(widgetResponse.statusCode()).thenReturn(200);
        when(widgetResponse.body()).thenReturn(widgetJson);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(inviteResponse)
                .thenReturn(widgetResponse);

        Optional<DiscordWidgetDataDto> result = discordWidgetService.getWidgetData(inviteCode);

        assertTrue(result.isPresent());
        DiscordWidgetDataDto data = result.get();
        assertEquals("Test Server", data.getServerName());
        assertEquals(100, data.getPresenceCount());
        assertEquals(1, data.getTopGames().size());
        assertEquals("Game1", data.getTopGames().get(0));
        assertEquals(2, data.getDisplayMembers().size());
        assertEquals("https://discord.com/invite/uuc", data.getInviteUrl());
        assertNotNull(data.getLogoUrl());
        assertTrue(data.getLogoUrl().contains(guildId));
    }

    @Test
    void shouldReturnEmptyWhenInviteCodeInvalid() throws Exception {
        String inviteCode = "invalid";
        when(inviteResponse.statusCode()).thenReturn(404);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(inviteResponse);

        Optional<DiscordWidgetDataDto> result = discordWidgetService.getWidgetData(inviteCode);

        assertFalse(result.isPresent());
    }

    @Test
    void shouldReturnEmptyWhenWidgetApiFails() throws Exception {
        String inviteCode = "uuc";
        String guildId = "454665524400619535";
        String inviteJson = "{\"guild\": {\"id\": \"" + guildId + "\", \"icon\": \"a_hash\"}}";

        when(inviteResponse.statusCode()).thenReturn(200);
        when(inviteResponse.body()).thenReturn(inviteJson);
        when(widgetResponse.statusCode()).thenReturn(500);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(inviteResponse)
                .thenReturn(widgetResponse);

        Optional<DiscordWidgetDataDto> result = discordWidgetService.getWidgetData(inviteCode);

        assertFalse(result.isPresent());
    }
}
