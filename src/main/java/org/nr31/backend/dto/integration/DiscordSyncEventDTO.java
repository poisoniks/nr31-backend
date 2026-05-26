package org.nr31.backend.dto.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscordSyncEventDTO {
    private String discordId;
    private String name;
    private String description;
    private Instant start;
    private Instant end;
    private String serverName;
    private String rrule;
    private String timezone;
    private List<DiscordSyncExceptionDTO> exceptions;
}
