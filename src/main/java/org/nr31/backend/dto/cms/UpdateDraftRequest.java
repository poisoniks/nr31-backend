package org.nr31.backend.dto.cms;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request to update a draft page revision")
public class UpdateDraftRequest {
    
    @NotNull(message = "Version must not be null")
    @Schema(description = "Current version number for optimistic locking", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer version;
    
    @NotNull(message = "Layout data must not be null")
    @Valid
    @Schema(description = "Complete layout data containing slots and widgets", requiredMode = Schema.RequiredMode.REQUIRED)
    private LayoutDataDto layoutData;
}
