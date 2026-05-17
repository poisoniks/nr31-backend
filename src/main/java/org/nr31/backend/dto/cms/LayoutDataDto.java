package org.nr31.backend.dto.cms;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.nr31.backend.validation.UniqueWidgetIds;
import org.nr31.backend.validation.ValidAttachments;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@UniqueWidgetIds
@ValidAttachments
@Schema(description = "Complete layout data for a page")
public class LayoutDataDto {

    @NotEmpty(message = "Layout must contain at least one slot")
    @Valid
    @Schema(description = "List of slots in the page layout", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<SlotDto> slots;
}
