package org.nr31.backend.repository;

import org.nr31.backend.model.KbArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KbArticleRepository extends JpaRepository<KbArticle, Long> {

    Optional<KbArticle> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByFolderId(Long folderId);

    Page<KbArticle> findByFolderIdOrderByCreatedAtDesc(Long folderId, Pageable pageable);

    @Query(value = """
            SELECT a.* FROM kb_articles a
            WHERE a.search_vector @@ websearch_to_tsquery('simple', :query)
               OR extract_localized_text(a.title) % :query
            ORDER BY
               ts_rank(a.search_vector, websearch_to_tsquery('simple', :query)) DESC,
               similarity(extract_localized_text(a.title), :query) DESC
            LIMIT 20
            """, nativeQuery = true)
    List<KbArticle> searchArticles(@Param("query") String query);
}
