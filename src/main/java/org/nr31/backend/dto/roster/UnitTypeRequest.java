package org.nr31.backend.dto.roster;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nr31.backend.validation.ValidLocalizedString;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request representation for a unit type")
public class UnitTypeRequest {

    @Schema(description = "Localized unit type name", example = "{\"en\": \"Alpha Squad\", \"uk\": \"Загін Альфа\"}")
    @ValidLocalizedString
    private Map<String, String> name;

    @Schema(description = "Localized unit type description", example = "{\"en\": \"Special operations squad\", \"uk\": \"Загін спеціального призначення\"}")
    @ValidLocalizedString
    private Map<String, String> description;

    @Schema(description = "UUID of the custom icon file to associate with this unit type", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID customIcon;
}
