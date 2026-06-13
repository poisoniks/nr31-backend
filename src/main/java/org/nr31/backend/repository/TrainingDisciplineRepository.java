package org.nr31.backend.repository;

import org.nr31.backend.model.TrainingDiscipline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrainingDisciplineRepository extends JpaRepository<TrainingDiscipline, Long> {
    Optional<TrainingDiscipline> findByName(String name);
}
