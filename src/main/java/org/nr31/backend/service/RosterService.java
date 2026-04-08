package org.nr31.backend.service;

import org.nr31.backend.dto.EventTypeDTO;
import org.nr31.backend.dto.EventTypeRequest;
import org.nr31.backend.dto.UnitTypeDTO;
import org.nr31.backend.dto.UnitTypeRequest;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RosterService {

    Page<UnitTypeDTO> getAllUnitTypes(Pageable pageable);

    Optional<UnitTypeDTO> getUnitTypeById(Long id);

    UnitTypeDTO createUnitType(UnitTypeRequest request);

    UnitTypeDTO updateUnitType(Long id, UnitTypeRequest request);

    void deleteUnitType(Long id);

    Page<EventTypeDTO> getAllEventTypes(Pageable pageable);

    Optional<EventTypeDTO> getEventTypeById(Long id);

    EventTypeDTO createEventType(EventTypeRequest request);

    EventTypeDTO updateEventType(Long id, EventTypeRequest request);

    void deleteEventType(Long id);
}
