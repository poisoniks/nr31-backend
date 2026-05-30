package org.nr31.backend.integration.discord.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DiscordEventExceptionDTO {
    private String exceptionId;
    private String eventId;
    private boolean isCancelled;
    private Instant exceptionDate;
    private Instant newStart;
    private Instant newEnd;
}
