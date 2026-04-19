package org.nr31.backend.repository;

import org.nr31.backend.model.FileMetadata;
import org.nr31.backend.model.FileScope;
import org.nr31.backend.model.MediaFolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {
    @Query("SELECT COALESCE(SUM(f.sizeBytes), 0) FROM FileMetadata f WHERE f.uploader.id = :userId")
    long sumSizeBytesByUploaderId(@Param("userId") Long userId);

    @Query("SELECT f FROM FileMetadata f WHERE f.scope = org.nr31.backend.model.FileScope.ATTACHMENT " +
           "AND f.createdAt < :threshold " +
           "AND f.id NOT IN (SELECT u.customIcon.id FROM UnitType u WHERE u.customIcon IS NOT NULL) " +
           "AND f.id NOT IN (SELECT e.customIcon.id FROM EventType e WHERE e.customIcon IS NOT NULL)")
    List<FileMetadata> findOrphanedAttachments(@Param("threshold") Instant threshold);

    @Query("SELECT DISTINCT f.storedName FROM FileMetadata f WHERE f.storedName IN :names")
    Set<String> findReferencedStoredNames(@Param("names") Collection<String> names);

    Page<FileMetadata> findByScopeAndFolder(FileScope scope, MediaFolder folder, Pageable pageable);

    Page<FileMetadata> findByScopeAndFolderIsNull(FileScope scope, Pageable pageable);

    boolean existsByFolder(MediaFolder folder);
}
