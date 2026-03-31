package org.nr31.backend.integration.discord.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response object containing discord bot status")
public class DiscordBotStatusResponse {
    @Schema(description = "Current status of discord bot", example = "OFFLINE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;
}
