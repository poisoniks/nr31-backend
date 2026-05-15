package org.nr31.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.nr31.backend.dto.cms.LayoutDataDto;
import org.nr31.backend.dto.cms.SlotDto;
import org.nr31.backend.dto.cms.WidgetDto;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Validator for {@link UniqueWidgetIds} constraint.
 * Recursively extracts all widget IDs from {@link LayoutDataDto} and ensures
 * they are unique.
 */
public class UniqueWidgetIdsValidator implements ConstraintValidator<UniqueWidgetIds, LayoutDataDto> {

    @Override
    public boolean isValid(LayoutDataDto layoutData, ConstraintValidatorContext context) {
        if (layoutData == null || layoutData.getSlots() == null) {
            return true;
        }

        Set<UUID> ids = new HashSet<>();
        for (SlotDto slot : layoutData.getSlots()) {
            if (slot.getWidgets() != null) {
                for (WidgetDto widget : slot.getWidgets()) {
                    UUID id = widget.getId();
                    if (id != null) {
                        if (!ids.add(id)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
}
