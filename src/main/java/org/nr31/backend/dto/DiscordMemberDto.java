package org.nr31.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A Discord member currently online")
public class DiscordMemberDto {

    @Schema(description = "Discord member ID", example = "1234567890")
    private String id;

    @Schema(description = "Username", example = "Pikachu")
    private String username;

    @Schema(description = "Avatar URL", example = "https://cdn.discordapp.com/widget-avatars/...")
    private String avatarUrl;

    @Schema(description = "Online status", example = "online")
    private String status;

    @Schema(description = "Name of the game currently being played", example = "Minecraft")
    private String gameName;
}
