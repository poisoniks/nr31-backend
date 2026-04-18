package org.nr31.backend.service;

import org.nr31.backend.dto.FileUploadResponse;
import org.nr31.backend.model.FileMetadata;
import org.nr31.backend.model.FileScope;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

public interface FileStorageService {
    FileUploadResponse storeFile(MultipartFile file, String uploaderUsername, FileScope scope);
    void deleteFile(UUID fileId);
    FileMetadata resolveFile(UUID fileId);
    void purgeOrphanedAttachments(Instant threshold);
    void purgeOrphanedPhysicalFiles();
}
