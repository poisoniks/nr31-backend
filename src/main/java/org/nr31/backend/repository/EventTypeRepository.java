package org.nr31.backend.repository;

import org.nr31.backend.model.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.QueryHints;
import org.hibernate.jpa.HibernateHints;
import java.util.Optional;

@Repository
public interface EventTypeRepository extends JpaRepository<EventType, Long> {

    @Override
    @EntityGraph(attributePaths = "customIcon")
    @QueryHints(@QueryHint(name = HibernateHints.HINT_CACHEABLE, value = "true"))
    Page<EventType> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "customIcon")
    @QueryHints(@QueryHint(name = HibernateHints.HINT_CACHEABLE, value = "true"))
    Optional<EventType> findById(Long id);
}
