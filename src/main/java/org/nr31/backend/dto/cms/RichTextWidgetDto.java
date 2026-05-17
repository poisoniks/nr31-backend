package org.nr31.backend.dto.cms;

import tools.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nr31.backend.annotation.LocalizedField;
import org.nr31.backend.validation.ValidRichTextSize;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Rich text widget with TipTap JSON AST support")
public class RichTextWidgetDto extends WidgetDto {

    @NotNull(message = "Body content must not be null")
    @Valid
    @ValidRichTextSize
    @LocalizedField
    @Schema(
            description = "Localized TipTap JSON AST content (max size per locale enforced by AppConfig key 'cms.richtext.max_size_bytes')",
            example = """
                    {
                      "en": {
                        "type": "doc",
                        "content": [
                          {
                            "type": "paragraph",
                            "content": [
                              {"type": "text", "text": "Welcome to "},
                              {"type": "text", "marks": [{"type": "bold"}], "text": "Nr.31 FKR"}
                            ]
                          }
                        ]
                      },
                      "uk": {
                        "type": "doc",
                        "content": [
                          {
                            "type": "paragraph",
                            "content": [
                              {"type": "text", "text": "Ласкаво просимо до "},
                              {"type": "text", "marks": [{"type": "bold"}], "text": "Nr.31 FKR"}
                            ]
                          }
                        ]
                      }
                    }
                    """,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Map<String, JsonNode> bodyContent;

    @JsonIgnore
    public Set<UUID> extractAttachmentIds() {
        Set<UUID> ids = new HashSet<>();
        if (bodyContent == null) return ids;

        bodyContent.values().forEach(node -> traverseForAttachments(node, ids));
        return ids;
    }

    private void traverseForAttachments(JsonNode node, Set<UUID> ids) {
        if (node.isObject()) {
            if (node.has("type") && "fileAttachment".equals(node.get("type").asString())) {
                JsonNode attrs = node.get("attrs");
                if (attrs != null && attrs.has("id")) {
                    try {
                        ids.add(UUID.fromString(attrs.get("id").asString()));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
        node.forEach(child -> traverseForAttachments(child, ids));
    }
}
