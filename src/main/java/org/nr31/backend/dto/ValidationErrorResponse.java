package org.nr31.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValidationErrorResponse {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<String, String> details;
    private String message;
    private LocalDateTime timestamp;
}
