package org.nr31.backend.repository;

import jakarta.persistence.QueryHint;
import org.nr31.backend.model.SupportedLocale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface SupportedLocaleRepository extends JpaRepository<SupportedLocale, Long> {
    @QueryHints(@QueryHint(name = org.hibernate.jpa.AvailableHints.HINT_CACHEABLE, value = "true"))
    long countByCodeIn(Collection<String> codes);
}
