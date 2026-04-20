package org.nr31.backend.repository;

import org.nr31.backend.model.PageRevision;
import org.nr31.backend.model.RevisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PageRevisionRepository extends JpaRepository<PageRevision, Long> {
    
    Optional<PageRevision> findByPageIdAndStatus(Long pageId, RevisionStatus status);
    
    @Query("SELECT pr FROM PageRevision pr JOIN FETCH pr.page WHERE pr.page.slug = :slug AND pr.status = :status")
    Optional<PageRevision> findByPageSlugAndStatusWithPage(@Param("slug") String slug, @Param("status") RevisionStatus status);
}
