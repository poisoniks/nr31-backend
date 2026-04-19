package org.nr31.backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.nr31.backend.dto.EventTypeDTO;
import org.nr31.backend.dto.EventTypeRequest;
import org.nr31.backend.dto.UnitTypeDTO;
import org.nr31.backend.dto.UnitTypeRequest;
import org.nr31.backend.dto.ErrorCode;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.model.EventType;
import org.nr31.backend.model.FileMetadata;
import org.nr31.backend.model.UnitType;
import org.nr31.backend.repository.EventTypeRepository;
import org.nr31.backend.repository.FileMetadataRepository;
import org.nr31.backend.repository.UnitTypeRepository;
import org.nr31.backend.service.RosterService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RosterServiceImpl implements RosterService {

    private final UnitTypeRepository unitTypeRepository;
    private final EventTypeRepository eventTypeRepository;
    private final FileMetadataRepository fileMetadataRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UnitTypeDTO> getAllUnitTypes(Pageable pageable) {
        return unitTypeRepository.findAll(pageable)
                .map(this::mapToUnitTypeDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UnitTypeDTO> getUnitTypeById(Long id) {
        return unitTypeRepository.findById(id).map(this::mapToUnitTypeDTO);
    }

    @Override
    @Transactional
    public UnitTypeDTO createUnitType(UnitTypeRequest request) {
        FileMetadata icon = resolveIcon(request.getCustomIcon());
        UnitType unitType = UnitType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .customIcon(icon)
                .build();
        unitType = unitTypeRepository.save(unitType);
        return mapToUnitTypeDTO(unitType);
    }

    @Override
    @Transactional
    @CacheEvict(value = "calendarEvents", allEntries = true)
    public UnitTypeDTO updateUnitType(Long id, UnitTypeRequest request) {
        UnitType unitType = unitTypeRepository.findById(id)
                .orElseThrow(() -> new ElementNotFoundException("UnitType not found", ErrorCode.UNIT_TYPE_NOT_FOUND, Map.of("id", id)));

        unitType.setName(request.getName());
        unitType.setDescription(request.getDescription());
        unitType.setCustomIcon(resolveIcon(request.getCustomIcon()));
        unitType = unitTypeRepository.save(unitType);

        return mapToUnitTypeDTO(unitType);
    }

    @Override
    @Transactional
    @CacheEvict(value = "calendarEvents", allEntries = true)
    public void deleteUnitType(Long id) {
        if (!unitTypeRepository.existsById(id)) {
            throw new ElementNotFoundException("UnitType not found", ErrorCode.UNIT_TYPE_NOT_FOUND, Map.of("id", id));
        }
        unitTypeRepository.deleteById(id);
    }

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

    private UnitTypeDTO mapToUnitTypeDTO(UnitType entity) {
        if (entity == null) {
            return null;
        }
        Hibernate.initialize(entity);
        return UnitTypeDTO.builder()
                .id(entity.getId())
                .name(entity.getName() != null ? new HashMap<>(entity.getName()) : null)
                .description(entity.getDescription() != null ? new HashMap<>(entity.getDescription()) : null)
                .customIcon(entity.getCustomIcon() != null ? entity.getCustomIcon().getId() : null)
                .build();
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
