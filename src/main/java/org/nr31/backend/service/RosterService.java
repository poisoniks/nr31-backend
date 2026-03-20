package org.nr31.backend.service;

import org.nr31.backend.dto.EventTypeDTO;
import org.nr31.backend.dto.EventTypeRequest;
import org.nr31.backend.dto.UnitTypeDTO;
import org.nr31.backend.dto.UnitTypeRequest;

import java.util.List;
import java.util.Optional;

public interface RosterService {

    List<UnitTypeDTO> getAllUnitTypes();
    Optional<UnitTypeDTO> getUnitTypeById(Long id);
    UnitTypeDTO createUnitType(UnitTypeRequest request);
    UnitTypeDTO updateUnitType(Long id, UnitTypeRequest request);
    void deleteUnitType(Long id);

    List<EventTypeDTO> getAllEventTypes();
    Optional<EventTypeDTO> getEventTypeById(Long id);
    EventTypeDTO createEventType(EventTypeRequest request);
    EventTypeDTO updateEventType(Long id, EventTypeRequest request);
    void deleteEventType(Long id);
}
