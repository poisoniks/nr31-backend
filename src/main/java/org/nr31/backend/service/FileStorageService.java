package org.nr31.backend.service;

import org.nr31.backend.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface FileStorageService {
    FileUploadResponse storeFile(MultipartFile file, String uploaderUsername);
    void deleteFile(UUID fileId);
    void deleteOldPendingFiles(java.time.Instant before);
}
