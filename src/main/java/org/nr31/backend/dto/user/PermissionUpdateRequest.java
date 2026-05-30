package org.nr31.backend.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

import org.nr31.backend.validation.ValidLocalizedString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for updating a permission's description")
public class PermissionUpdateRequest {
    @ValidLocalizedString
    @Schema(description = "Localized description of the permission", example = "{\"en\": \"Manages roles\", \"uk\": \"Керує ролями\"}")
    private Map<String, String> description;
}
