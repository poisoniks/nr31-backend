package org.nr31.backend.service;

import org.nr31.backend.dto.media.FileMetadataDTO;
import org.nr31.backend.dto.media.FileUploadResponse;
import org.nr31.backend.dto.media.LibraryFileUpdateRequest;
import org.nr31.backend.model.FileMetadata;
import org.nr31.backend.model.FileScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface FileStorageService {
    FileUploadResponse storeFile(MultipartFile file, String uploaderUsername, FileScope scope);

    FileUploadResponse storeLibraryFile(MultipartFile file, String uploaderUsername, UUID folderId);

    Page<FileMetadataDTO> listLibraryFiles(UUID folderId, Pageable pageable);

    FileMetadataDTO updateLibraryFile(UUID id, LibraryFileUpdateRequest request);

    void deleteFile(UUID fileId);

    FileMetadata resolveFile(UUID fileId);

    void purgeOrphanedAttachments(Instant threshold);

    void purgeOrphanedPhysicalFiles();

    Set<String> getAllowedMimeTypes();

    FileUploadResponse storeFile(byte[] bytes, String originalName, String contentType, String uploaderUsername, FileScope scope);
}
