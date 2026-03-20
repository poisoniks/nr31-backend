package org.nr31.backend.integration.discord.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DiscordDeleteDTO {
    private String id;
    private String eventId;
    private String guildId;
}
