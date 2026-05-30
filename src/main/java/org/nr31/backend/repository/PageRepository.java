package org.nr31.backend.repository;

import org.nr31.backend.model.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    
    Optional<Page> findBySlug(String slug);
}
