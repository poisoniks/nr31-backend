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
    @JsonSubTypes.Type(value = HeroWidgetDto.class, name = "hero"),
    @JsonSubTypes.Type(value = RichTextWidgetDto.class, name = "richtext"),
    @JsonSubTypes.Type(value = NextEventWidgetDto.class, name = "nextevent"),
    @JsonSubTypes.Type(value = NewsFeedWidgetDto.class, name = "newsfeed"),
    @JsonSubTypes.Type(value = YoutubeWidgetDto.class, name = "youtube"),
    @JsonSubTypes.Type(value = DiscordWidgetDto.class, name = "discord")
})
public abstract class WidgetDto {
    // Note: The 'type' field is managed automatically by Jackson's @JsonTypeInfo
    // Do not add an explicit 'type' field here
}
