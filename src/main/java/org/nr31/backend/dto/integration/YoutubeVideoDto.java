package org.nr31.backend.dto.integration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Cached YouTube video data for a channel")
public class YoutubeVideoDto {

    @Schema(description = "YouTube video ID", example = "dQw4w9WgXcQ")
    private String videoId;

    @Schema(description = "Video title", example = "Regiment Training Session #42")
    private String title;

    @Schema(description = "Direct link to the video", example = "https://www.youtube.com/watch?v=dQw4w9WgXcQ")
    private String link;

    @Schema(description = "Video publish date")
    private OffsetDateTime published;

    @Schema(description = "Channel/author name", example = "Nr.31 Feldkanonenregiment")
    private String author;

    @Schema(description = "Whether the video is a YouTube Short")
    private boolean isShort;

    @Schema(description = "YouTube channel ID this video belongs to", example = "UCbU41G2hhiwdn-gFFRqZN4w")
    private String channelId;
}
