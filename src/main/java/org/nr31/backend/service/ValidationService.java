package org.nr31.backend.service;

import org.nr31.backend.dto.cms.LayoutDataDto;
import org.nr31.backend.dto.cms.SlotRestrictionsDto;
import org.nr31.backend.dto.cms.UpdateSlotRestrictionsRequest;

public interface ValidationService {
    
    /**
     * Validates layout data against slot restrictions.
     * Uses cached slot restrictions for performance.
     * 
     * @param layoutData the layout data to validate
     * @throws org.nr31.backend.exception.ValidationException if validation fails
     */
    void validateLayout(LayoutDataDto layoutData);
    
    /**
     * Retrieves cached slot restrictions.
     * 
     * @return the current slot restrictions
     */
    SlotRestrictionsDto getSlotRestrictions();
    
    /**
     * Updates slot restrictions and evicts cache.
     * 
     * @param request the new slot restrictions
     */
    void updateSlotRestrictions(UpdateSlotRestrictionsRequest request);
}
