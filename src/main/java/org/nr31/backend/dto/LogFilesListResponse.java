package org.nr31.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response object containing list of available log files")
public class LogFilesListResponse {
    @Schema(description = "Array of available log files", example = "[/app/logs/application.log]")
    private List<String> logFiles;
}
