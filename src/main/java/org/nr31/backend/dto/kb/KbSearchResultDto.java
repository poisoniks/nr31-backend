package org.nr31.backend.dto.kb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KbSearchResultDto {
    private KbArticleSummaryDto article;
    private List<KbFolderDto> breadcrumbs;
}
