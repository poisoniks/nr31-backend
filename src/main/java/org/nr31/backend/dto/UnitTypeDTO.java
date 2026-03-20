package org.nr31.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO representing a participating military unit")
public class UnitTypeDTO {

    @Schema(description = "Unique identifier of the unit type", example = "1")
    private Long id;

    @Schema(description = "Localized name of the unit type", example = "{\"en\": \"Alpha Squad\", \"uk\": \"Загін Альфа\"}")
    private Map<String, String> name;

    @Schema(description = "Localized description of the unit type", example = "{\"en\": \"Special operations squad\", \"uk\": \"Загін спеціального призначення\"}")
    private Map<String, String> description;
}
