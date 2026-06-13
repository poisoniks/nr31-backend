package org.nr31.backend.service;

import org.nr31.backend.dto.calendar.EventTypeDTO;
import org.nr31.backend.dto.calendar.EventTypeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface EventTypeService {

    Page<EventTypeDTO> getAllEventTypes(Pageable pageable);

    Optional<EventTypeDTO> getEventTypeById(Long id);

    EventTypeDTO createEventType(EventTypeRequest request);

    EventTypeDTO updateEventType(Long id, EventTypeRequest request);

    void deleteEventType(Long id);
}
