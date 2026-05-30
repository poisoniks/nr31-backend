package org.nr31.backend.dto.cms;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Discord widget that displays server presence and member activity")
public class DiscordWidgetDto extends WidgetDto {

    @NotBlank(message = "Invite code must not be blank")
    @Pattern(regexp = "^[a-zA-Z0-9-]{2,32}$", message = "Invite code must be a valid Discord invite code")
    @Schema(description = "Discord server invite code (e.g. 'uuc' from discord.com/invite/uuc)",
            example = "uuc",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String inviteCode;
}
