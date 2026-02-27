package org.nr31.backend.repository;

import org.nr31.backend.model.SupportedLocale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface SupportedLocaleRepository extends JpaRepository<SupportedLocale, Long> {
    long countByCodeIn(Collection<String> codes);
}
