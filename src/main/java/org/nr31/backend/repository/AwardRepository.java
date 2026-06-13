package org.nr31.backend.repository;

import org.nr31.backend.model.Award;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AwardRepository extends JpaRepository<Award, Long> {
    Optional<Award> findByAbbreviation(String abbreviation);
    Optional<Award> findByName(String name);
}
