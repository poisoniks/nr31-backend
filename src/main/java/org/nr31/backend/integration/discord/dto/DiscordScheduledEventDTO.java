package org.nr31.backend.integration.discord.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class DiscordScheduledEventDTO {
    private String id;
    private String guildId;
    private String name;
    private String description;
    private Instant scheduledStartTime;
    private Instant scheduledEndTime;
    private String serverName;
    private String rrule;
    private List<DiscordEventExceptionDTO> exceptions;
}
