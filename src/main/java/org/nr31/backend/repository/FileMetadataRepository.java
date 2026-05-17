package org.nr31.backend.repository;

import org.nr31.backend.model.FileMetadata;
import org.nr31.backend.model.FileScope;
import org.nr31.backend.model.MediaFolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {
    @Override
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    boolean existsById(UUID id);

    @Query("SELECT COALESCE(SUM(f.sizeBytes), 0) FROM FileMetadata f WHERE f.uploader.id = :userId")
    long sumSizeBytesByUploaderId(@Param("userId") Long userId);

    Optional<FileMetadata> findByStoredNameAndUploaderIdAndScope(String storedName, Long uploaderId, FileScope scope);

    @Query(value = """
                SELECT f.* FROM files_metadata f
                WHERE f.scope = 'ATTACHMENT'
                  AND f.created_at < :threshold
                  AND f.id NOT IN (SELECT custom_icon_id FROM unit_types WHERE custom_icon_id IS NOT NULL)
                  AND f.id NOT IN (SELECT custom_icon_id FROM event_types WHERE custom_icon_id IS NOT NULL)
                  AND NOT EXISTS (
                      SELECT 1 FROM page_revisions pr
                      WHERE f.id = ANY(
                          ARRAY(
                              SELECT CAST(jsonb_path_query(pr.layout_data, '$.** ? (@.type == "fileAttachment").attrs.id') AS uuid)
                          )
                      )
                  )
            """, nativeQuery = true)
    List<FileMetadata> findOrphanedAttachments(@Param("threshold") Instant threshold);

    @Query(value = """
                SELECT f.id FROM files_metadata f
                WHERE f.id IN :fileIds
                  AND EXISTS (
                      SELECT 1 FROM page_revisions pr
                      WHERE f.id = ANY(
                          ARRAY(
                              SELECT CAST(jsonb_path_query(pr.layout_data, '$.** ? (@.type == "fileAttachment").attrs.id') AS uuid)
                          )
                      )
                  )
            """, nativeQuery = true)
    Set<UUID> findReferencedAttachmentIds(@Param("fileIds") Collection<UUID> fileIds);

    @Query("SELECT DISTINCT f.storedName FROM FileMetadata f WHERE f.storedName IN :names")
    Set<String> findReferencedStoredNames(@Param("names") Collection<String> names);

    Page<FileMetadata> findByScopeAndFolder(FileScope scope, MediaFolder folder, Pageable pageable);

    Page<FileMetadata> findByScopeAndFolderIsNull(FileScope scope, Pageable pageable);

    boolean existsByFolder(MediaFolder folder);
}
