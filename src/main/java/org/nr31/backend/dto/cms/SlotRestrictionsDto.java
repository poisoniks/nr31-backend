package org.nr31.backend.dto.cms;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Configuration defining which widget types are allowed in each slot type")
public class SlotRestrictionsDto {
    
    @Schema(
        description = "Map of slot types to their allowed widget types",
        example = "{\"hero\": [\"text\", \"image\"], \"sidebar\": [\"text\", \"video\"], \"content\": [\"text\", \"image\", \"video\", \"embed\"]}",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Map<String, List<String>> restrictions;
}
