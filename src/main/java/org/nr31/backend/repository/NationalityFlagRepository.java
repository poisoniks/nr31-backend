package org.nr31.backend.repository;

import org.nr31.backend.model.NationalityFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NationalityFlagRepository extends JpaRepository<NationalityFlag, Long> {
    Optional<NationalityFlag> findByFlagFileStoredName(String storedName);
}
