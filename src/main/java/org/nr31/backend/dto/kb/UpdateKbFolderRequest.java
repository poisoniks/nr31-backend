package org.nr31.backend.dto.kb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nr31.backend.validation.ValidLocalizedString;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateKbFolderRequest {

    @ValidLocalizedString
    private Map<String, String> name;

    private Long parentId;

    private Boolean restricted;
}
