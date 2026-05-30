package org.nr31.backend.dto.integration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cached Discord widget data for a server")
public class DiscordWidgetDataDto {

    @Schema(description = "Discord server name", example = "Єдине Україномовне Ком'юніті")
    private String serverName;

    @Schema(description = "Server logo URL", example = "https://cdn.discordapp.com/icons/...")
    private String logoUrl;

    @Schema(description = "Total number of online members", example = "4080")
    private int presenceCount;

    @Schema(description = "Top 3 games being played", example = "[\"World of Tanks\", \"Counter-Strike 2\", \"Code\"]")
    private List<String> topGames;

    @Schema(description = "List of members to display (max 15)", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<DiscordMemberDto> displayMembers;

    @Schema(description = "Number of online members not included in displayMembers", example = "4065")
    private int moreCount;

    @Schema(description = "Direct invite link to the server", example = "https://discord.com/invite/uuc")
    private String inviteUrl;
}
