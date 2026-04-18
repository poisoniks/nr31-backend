package org.nr31.backend.repository;

import org.nr31.backend.model.UnitType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.Optional;

@Repository
public interface UnitTypeRepository extends JpaRepository<UnitType, Long> {
    
    @Override
    @EntityGraph(attributePaths = "customIcon")
    Page<UnitType> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "customIcon")
    Optional<UnitType> findById(Long id);
}
