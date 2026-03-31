package org.nr31.backend.repository;

import org.nr31.backend.model.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.nr31.backend.model.FileStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {
    @Query("SELECT COALESCE(SUM(f.sizeBytes), 0) FROM FileMetadata f WHERE f.uploader.id = :userId")
    long sumSizeBytesByUploaderId(@Param("userId") Long userId);

    List<FileMetadata> findByStatusAndCreatedAtBefore(FileStatus status, Instant createdAt);
}
