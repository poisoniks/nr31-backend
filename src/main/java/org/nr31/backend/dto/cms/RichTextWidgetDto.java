package org.nr31.backend.dto.cms;

import tools.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.nr31.backend.validation.ValidRichTextSize;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Rich text widget with TipTap JSON AST support")
public class RichTextWidgetDto extends WidgetDto {
    
    @NotNull(message = "Body content must not be null")
    @Valid
    @ValidRichTextSize
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
}
