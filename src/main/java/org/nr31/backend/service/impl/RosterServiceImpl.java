package org.nr31.backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.nr31.backend.dto.roster.UnitTypeDTO;
import org.nr31.backend.dto.roster.UnitTypeRequest;
import org.nr31.backend.dto.common.ErrorCode;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.model.FileMetadata;
import org.nr31.backend.model.UnitType;
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
}
