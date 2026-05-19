package org.nr31.backend.repository;

import org.nr31.backend.model.KbFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KbFolderRepository extends JpaRepository<KbFolder, Long> {

    List<KbFolder> findByParentIsNullOrderByName();

    List<KbFolder> findByParentIdOrderByName(Long parentId);

    Optional<KbFolder> findBySlugAndParentIsNull(String slug);

    Optional<KbFolder> findBySlugAndParentId(String slug, Long parentId);

    Optional<KbFolder> findFirstBySlug(String slug);

    boolean existsBySlugAndParentIsNull(String slug);

    boolean existsBySlugAndParentIsNullAndIdNot(String slug, Long excludeId);

    boolean existsBySlugAndParentId(String slug, Long parentId);

    boolean existsBySlugAndParentIdAndIdNot(String slug, Long parentId, Long excludeId);

    boolean existsByParentId(Long parentId);

    @Query(value = """
        WITH RECURSIVE folder_path AS (
            SELECT id, name, slug, parent_id, is_restricted, created_at, 1 AS depth
            FROM kb_folders
            WHERE id = :folderId
            UNION ALL
            SELECT f.id, f.name, f.slug, f.parent_id, f.is_restricted, f.created_at, depth + 1
            FROM kb_folders f
            JOIN folder_path fp ON f.id = fp.parent_id
        )
        SELECT id, name, slug, parent_id, is_restricted, created_at
        FROM folder_path
        ORDER BY depth DESC
        """, nativeQuery = true)
    List<KbFolder> findAncestorChain(@Param("folderId") Long folderId);
}
