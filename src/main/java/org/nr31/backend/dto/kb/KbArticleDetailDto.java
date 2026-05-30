package org.nr31.backend.dto.kb;

import tools.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbArticleDetailDto {
    private Long id;
    private Long folderId;
    private Map<String, String> folderName;
    private Long authorId;
    private String authorName;
    private Map<String, String> title;
    private String slug;
    private JsonNode content;
    private List<KbFolderDto> breadcrumbs;
    private Instant createdAt;
    private Instant updatedAt;
}
