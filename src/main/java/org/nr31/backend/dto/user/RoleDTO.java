package org.nr31.backend.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO representing an user role")
public class RoleDTO {
    @Schema(description = "Unique identifier of the role", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "Role identifier name", example = "ROLE_ADMIN", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Localized name of the role", example = "{\"en\": \"Admin\", \"uk\": \"Адміністратор\"}")
    private Map<String, String> localizedName;

    @Schema(description = "Permissions assigned to this role", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<PermissionDTO> permissions;

    @Schema(description = "File upload quota in bytes", example = "104857600")
    private Long filesUploadQuotaBytes;
}
