package org.nr31.backend.dto.kb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbFolderDto {
    private Long id;
    private Map<String, String> name;
    private String slug;
    private boolean restricted;
}
