package org.nr31.backend.dto.cms;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Base class for all widgets")
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = TextWidgetDto.class, name = "text"),
    @JsonSubTypes.Type(value = ImageWidgetDto.class, name = "image"),
    @JsonSubTypes.Type(value = VideoWidgetDto.class, name = "video"),
    @JsonSubTypes.Type(value = EmbedWidgetDto.class, name = "embed")
})
public abstract class WidgetDto {
    // Note: The 'type' field is managed automatically by Jackson's @JsonTypeInfo
    // Do not add an explicit 'type' field here
}
