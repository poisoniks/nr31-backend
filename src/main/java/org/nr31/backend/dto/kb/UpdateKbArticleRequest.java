package org.nr31.backend.dto.kb;

import tools.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nr31.backend.validation.ValidLocalizedString;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateKbArticleRequest {

    private Long folderId;

    @ValidLocalizedString
    private Map<String, String> title;

    private JsonNode content;
}
