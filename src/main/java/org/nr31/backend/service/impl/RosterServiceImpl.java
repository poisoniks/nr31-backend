package org.nr31.backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.nr31.backend.dto.EventTypeDTO;
import org.nr31.backend.dto.EventTypeRequest;
import org.nr31.backend.dto.UnitTypeDTO;
import org.nr31.backend.dto.UnitTypeRequest;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.model.EventType;
import org.nr31.backend.model.UnitType;
import org.nr31.backend.repository.EventTypeRepository;
import org.nr31.backend.repository.UnitTypeRepository;
import org.nr31.backend.service.RosterService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RosterServiceImpl implements RosterService {

    private final UnitTypeRepository unitTypeRepository;
    private final EventTypeRepository eventTypeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UnitTypeDTO> getAllUnitTypes() {
        return unitTypeRepository.findAll().stream()
                .map(this::mapToUnitTypeDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UnitTypeDTO> getUnitTypeById(Long id) {
        return unitTypeRepository.findById(id).map(this::mapToUnitTypeDTO);
    }

    @Override
    @Transactional
    public UnitTypeDTO createUnitType(UnitTypeRequest request) {
        UnitType unitType = UnitType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .customIcon(request.getCustomIcon())
                .build();
        unitType = unitTypeRepository.save(unitType);
        return mapToUnitTypeDTO(unitType);
    }

    @Override
    @Transactional
    public UnitTypeDTO updateUnitType(Long id, UnitTypeRequest request) {
        UnitType unitType = unitTypeRepository.findById(id)
                .orElseThrow(() -> new ElementNotFoundException("UnitType not found"));

        unitType.setName(request.getName());
        unitType.setDescription(request.getDescription());
        unitType.setCustomIcon(request.getCustomIcon());
        unitType = unitTypeRepository.save(unitType);

        return mapToUnitTypeDTO(unitType);
    }

    @Override
    @Transactional
    public void deleteUnitType(Long id) {
        if (!unitTypeRepository.existsById(id)) {
            throw new ElementNotFoundException("UnitType not found");
        }
        unitTypeRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventTypeDTO> getAllEventTypes() {
        return eventTypeRepository.findAll().stream()
                .map(this::mapToEventTypeDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EventTypeDTO> getEventTypeById(Long id) {
        return eventTypeRepository.findById(id).map(this::mapToEventTypeDTO);
    }

    @Override
    @Transactional
    public EventTypeDTO createEventType(EventTypeRequest request) {
        EventType eventType = EventType.builder()
                .name(request.getName())
                .customIcon(request.getCustomIcon())
                .build();
        eventType = eventTypeRepository.save(eventType);
        return mapToEventTypeDTO(eventType);
    }

    @Override
    @Transactional
    public EventTypeDTO updateEventType(Long id, EventTypeRequest request) {
        EventType eventType = eventTypeRepository.findById(id)
                .orElseThrow(() -> new ElementNotFoundException("EventType not found"));

        eventType.setName(request.getName());
        eventType.setCustomIcon(request.getCustomIcon());
        eventType = eventTypeRepository.save(eventType);

        return mapToEventTypeDTO(eventType);
    }

    @Override
    @Transactional
    public void deleteEventType(Long id) {
        if (!eventTypeRepository.existsById(id)) {
            throw new ElementNotFoundException("EventType not found");
        }
        eventTypeRepository.deleteById(id);
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
                .customIcon(entity.getCustomIcon())
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
                .customIcon(entity.getCustomIcon())
                .build();
    }
}
