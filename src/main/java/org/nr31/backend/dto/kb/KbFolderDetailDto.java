package org.nr31.backend.dto.kb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbFolderDetailDto {
    private Long id;
    private Map<String, String> name;
    private String slug;
    private boolean restricted;
    private List<KbFolderDto> subFolders;
    private Page<KbArticleSummaryDto> articles;
}
