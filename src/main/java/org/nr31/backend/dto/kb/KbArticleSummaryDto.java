package org.nr31.backend.dto.kb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbArticleSummaryDto {
    private Long id;
    private Map<String, String> title;
    private String slug;
    private Long authorId;
    private String authorName;
    private Instant createdAt;
    private Instant updatedAt;
}
