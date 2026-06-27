package org.nr31.backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.nr31.backend.dto.calendar.EventTypeDTO;
import org.nr31.backend.dto.calendar.EventTypeRequest;
import org.nr31.backend.dto.common.ErrorCode;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.model.EventType;
import org.nr31.backend.model.FileMetadata;
import org.nr31.backend.repository.EventTypeRepository;
import org.nr31.backend.repository.FileMetadataRepository;
import org.nr31.backend.service.EventTypeService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventTypeServiceImpl implements EventTypeService {

    private final EventTypeRepository eventTypeRepository;
    private final FileMetadataRepository fileMetadataRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<EventTypeDTO> getAllEventTypes(Pageable pageable) {
        return eventTypeRepository.findAll(pageable)
                .map(this::mapToEventTypeDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EventTypeDTO> getEventTypeById(Long id) {
        return eventTypeRepository.findById(id).map(this::mapToEventTypeDTO);
    }

    @Override
    @Transactional
    public EventTypeDTO createEventType(EventTypeRequest request) {
        FileMetadata icon = resolveIcon(request.getCustomIcon());
        EventType eventType = EventType.builder()
                .name(request.getName())
                .customIcon(icon)
                .attendanceWeight(request.getAttendanceWeight() != null ? request.getAttendanceWeight() : 1)
                .build();
        eventType = eventTypeRepository.save(eventType);
        return mapToEventTypeDTO(eventType);
    }

    @Override
    @Transactional
    @CacheEvict(value = "calendarEvents", allEntries = true)
    public EventTypeDTO updateEventType(Long id, EventTypeRequest request) {
        EventType eventType = eventTypeRepository.findById(id)
                .orElseThrow(() -> new ElementNotFoundException("EventType not found", ErrorCode.EVENT_TYPE_NOT_FOUND, Map.of("id", id)));

        eventType.setName(request.getName());
        eventType.setCustomIcon(resolveIcon(request.getCustomIcon()));
        if (request.getAttendanceWeight() != null) {
            eventType.setAttendanceWeight(request.getAttendanceWeight());
        }
        eventType = eventTypeRepository.save(eventType);

        return mapToEventTypeDTO(eventType);
    }

    @Override
    @Transactional
    @CacheEvict(value = "calendarEvents", allEntries = true)
    public void deleteEventType(Long id) {
        if (!eventTypeRepository.existsById(id)) {
            throw new ElementNotFoundException("EventType not found", ErrorCode.EVENT_TYPE_NOT_FOUND, Map.of("id", id));
        }
        eventTypeRepository.deleteById(id);
    }

    private FileMetadata resolveIcon(UUID iconId) {
        if (iconId == null) {
            return null;
        }
        return fileMetadataRepository.findById(iconId)
                .orElseThrow(() -> new ElementNotFoundException("Icon file not found", ErrorCode.FILE_NOT_FOUND, Map.of("id", iconId)));
    }

    private EventTypeDTO mapToEventTypeDTO(EventType entity) {
        if (entity == null) {
            return null;
        }
        Hibernate.initialize(entity);
        return EventTypeDTO.builder()
                .id(entity.getId())
                .name(entity.getName() != null ? new HashMap<>(entity.getName()) : null)
                .customIcon(entity.getCustomIcon() != null ? entity.getCustomIcon().getId() : null)
                .build();
    }
}
