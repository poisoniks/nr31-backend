package org.nr31.backend.dto.cms;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nr31.backend.model.RevisionStatus;

import java.time.OffsetDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response DTO representing a page revision")
public class PageResponseDto {
    
    @Schema(description = "Unique identifier of the page", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
    
    @Schema(description = "URL-friendly page identifier", example = "home", requiredMode = Schema.RequiredMode.REQUIRED)
    private String slug;
    
    @Schema(description = "Page title", example = "Home Page", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;
    
    @Schema(description = "Current version number for optimistic locking", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer version;
    
    @Schema(description = "Complete layout data containing slots and widgets", requiredMode = Schema.RequiredMode.REQUIRED)
    private LayoutDataDto layoutData;
    
    @Schema(description = "Status of the page revision (DRAFT, PUBLISHED, ARCHIVED)", example = "PUBLISHED", requiredMode = Schema.RequiredMode.REQUIRED)
    private RevisionStatus status;
    
    @Schema(description = "Timestamp when the revision was created", example = "2024-01-15T10:30:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private OffsetDateTime createdAt;
}
