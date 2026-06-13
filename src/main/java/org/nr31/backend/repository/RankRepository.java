package org.nr31.backend.repository;

import org.nr31.backend.model.Rank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RankRepository extends JpaRepository<Rank, Long> {
    Optional<Rank> findByAbbreviation(String abbreviation);
    Optional<Rank> findByName(String name);
}
