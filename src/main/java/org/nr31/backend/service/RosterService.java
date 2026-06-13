package org.nr31.backend.service;

import org.nr31.backend.dto.roster.UnitTypeDTO;
import org.nr31.backend.dto.roster.UnitTypeRequest;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RosterService {

    Page<UnitTypeDTO> getAllUnitTypes(Pageable pageable);

    Optional<UnitTypeDTO> getUnitTypeById(Long id);

    UnitTypeDTO createUnitType(UnitTypeRequest request);

    UnitTypeDTO updateUnitType(Long id, UnitTypeRequest request);

    void deleteUnitType(Long id);
}
