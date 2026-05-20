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

    // Level 1: Basic — Full-text search only (stems)
    @Query(value = """
            SELECT a.* FROM kb_articles a
            WHERE a.search_vector @@ websearch_to_tsquery('english', :query)
               OR a.search_vector @@ websearch_to_tsquery('simple', :query)
            ORDER BY
               GREATEST(
                   ts_rank(a.search_vector, websearch_to_tsquery('english', :query)),
                   ts_rank(a.search_vector, websearch_to_tsquery('simple', :query))
               ) DESC
            LIMIT 20
            """, nativeQuery = true)
    List<KbArticle> searchBasic(@Param("query") String query);

    // Level 2: Standard — FTS + prefix + word_similarity (fuzzy)
    @Query(value = """
            SELECT a.* FROM kb_articles a
            WHERE a.search_vector @@ websearch_to_tsquery('english', :query)
               OR a.search_vector @@ websearch_to_tsquery('simple', :query)
               OR a.search_vector @@ to_tsquery('simple',
                      regexp_replace(trim(:query), '\\s+', ':* & ', 'g') || ':*')
               OR word_similarity(:query, extract_localized_text(a.title)) > 0.3
               OR word_similarity(:query, extract_localized_text(a.plain_text_content)) > 0.3
            ORDER BY
               GREATEST(
                   ts_rank(a.search_vector, websearch_to_tsquery('english', :query)),
                   ts_rank(a.search_vector, websearch_to_tsquery('simple', :query))
               ) DESC,
               GREATEST(
                   word_similarity(:query, extract_localized_text(a.title)),
                   word_similarity(:query, extract_localized_text(a.plain_text_content))
               ) DESC
            LIMIT 20
            """, nativeQuery = true)
    List<KbArticle> searchStandard(@Param("query") String query);

    // Level 3: Full — everything above + ILIKE substring (infix/suffix)
    @Query(value = """
            SELECT a.* FROM kb_articles a
            WHERE a.search_vector @@ websearch_to_tsquery('english', :query)
               OR a.search_vector @@ websearch_to_tsquery('simple', :query)
               OR a.search_vector @@ to_tsquery('simple',
                      regexp_replace(trim(:query), '\\s+', ':* & ', 'g') || ':*')
               OR word_similarity(:query, extract_localized_text(a.title)) > 0.3
               OR word_similarity(:query, extract_localized_text(a.plain_text_content)) > 0.3
               OR extract_localized_text(a.title) ILIKE '%' || :query || '%'
               OR extract_localized_text(a.plain_text_content) ILIKE '%' || :query || '%'
            ORDER BY
               GREATEST(
                   ts_rank(a.search_vector, websearch_to_tsquery('english', :query)),
                   ts_rank(a.search_vector, websearch_to_tsquery('simple', :query))
               ) DESC,
               GREATEST(
                   word_similarity(:query, extract_localized_text(a.title)),
                   word_similarity(:query, extract_localized_text(a.plain_text_content))
               ) DESC
            LIMIT 20
            """, nativeQuery = true)
    List<KbArticle> searchFull(@Param("query") String query);
}
