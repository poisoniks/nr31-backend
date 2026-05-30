package org.nr31.backend.dto.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscordSyncExceptionDTO {
    private String exceptionId;
    private boolean isCancelled;
    private Instant exceptionDate;
    private Instant newStart;
    private Instant newEnd;
}
