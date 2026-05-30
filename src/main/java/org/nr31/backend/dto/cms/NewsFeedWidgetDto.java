package org.nr31.backend.dto.cms;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nr31.backend.annotation.LocalizedField;
import org.nr31.backend.validation.ValidLocalizedString;
import org.nr31.backend.validation.ValidNewsFeedItemCount;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "News feed widget for displaying recent articles")
public class NewsFeedWidgetDto extends WidgetDto {
    
    @NotNull(message = "Section title must not be null")
    @ValidLocalizedString
    @LocalizedField
    @Schema(description = "Localized section header", 
            example = "{\"en\": \"Latest News\", \"uk\": \"Останні новини\"}", 
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> sectionTitle;
    
    @NotNull(message = "Item count must not be null")
    @Min(value = 1, message = "Item count must be at least 1")
    @ValidNewsFeedItemCount
    @Schema(description = "Number of articles to fetch (max enforced by AppConfig key 'cms.newsfeed.max_items')", 
            example = "3", 
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer itemCount;
    
    @Schema(description = "Optional tag filter for news items", 
            example = "announcements", 
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String tagFilter;
}
