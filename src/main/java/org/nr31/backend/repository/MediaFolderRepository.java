package org.nr31.backend.repository;

import org.nr31.backend.model.MediaFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MediaFolderRepository extends JpaRepository<MediaFolder, UUID> {

    @Query("SELECT f FROM MediaFolder f LEFT JOIN FETCH f.parent WHERE f.id = :id")
    Optional<MediaFolder> findByIdWithParent(@Param("id") UUID id);

    @Query("SELECT f FROM MediaFolder f LEFT JOIN FETCH f.parent WHERE f.parent IS NULL ORDER BY f.name")
    List<MediaFolder> findTopLevelFolders();

    @Query("SELECT f FROM MediaFolder f LEFT JOIN FETCH f.parent WHERE f.parent.id = :parentId ORDER BY f.name")
    List<MediaFolder> findChildrenByParentId(@Param("parentId") UUID parentId);

    boolean existsByParentId(UUID parentId);
}
