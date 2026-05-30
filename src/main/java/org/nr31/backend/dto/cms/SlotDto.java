package org.nr31.backend.dto.cms;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "A slot that can contain widgets")
public class SlotDto {
    
    @NotBlank(message = "Slot type must not be blank")
    @Schema(description = "Type of the slot (e.g., hero, sidebar, content)", example = "hero", requiredMode = Schema.RequiredMode.REQUIRED)
    private String slotType;
    
    @NotEmpty(message = "Slot must contain at least one widget")
    @Valid
    @Schema(description = "List of widgets in this slot", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<WidgetDto> widgets;
}
