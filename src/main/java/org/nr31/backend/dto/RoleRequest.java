package org.nr31.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nr31.backend.validation.ValidLocalizedString;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for creating or updating a role")
public class RoleRequest {
    @NotBlank(message = "Role name cannot be blank")
    @Schema(description = "Name of the role", example = "ROLE_NEW", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @ValidLocalizedString
    @Schema(description = "Localized name of the role", example = "{\"en\": \"Admin\", \"uk\": \"Адміністратор\"}")
    private Map<String, String> localizedName;
}
