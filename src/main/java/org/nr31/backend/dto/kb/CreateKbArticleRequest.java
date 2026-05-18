package org.nr31.backend.dto.kb;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
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
public class CreateKbArticleRequest {

    @NotNull(message = "Folder ID is required")
    private Long folderId;

    @NotNull(message = "Title is required")
    @ValidLocalizedString
    private Map<String, String> title;

    @NotNull(message = "Content is required")
    private JsonNode content;
}
