package org.nr31.backend.dto.cms;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request to publish a draft page revision")
public class PublishDraftRequest {
    
    @NotNull(message = "Version must not be null")
    @Schema(description = "Current version number for optimistic locking", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer version;
}
