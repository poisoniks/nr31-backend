package org.nr31.backend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.dto.FileUploadResponse;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.exception.FileStorageException;
import org.nr31.backend.model.FileMetadata;
import org.nr31.backend.model.User;
import org.nr31.backend.repository.FileMetadataRepository;
import org.nr31.backend.repository.UserRepository;
import org.nr31.backend.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class LocalDriveStorageService implements FileStorageService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp"
    );

    private static final String UPLOADS_URL_PREFIX = "/uploads/";

    private final Path uploadDir;
    private final FileMetadataRepository fileMetadataRepository;
    private final UserRepository userRepository;

    public LocalDriveStorageService(
            @Value("${app.uploads.dir:/app/uploads}") String uploadDir,
            FileMetadataRepository fileMetadataRepository,
            UserRepository userRepository) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.fileMetadataRepository = fileMetadataRepository;
        this.userRepository = userRepository;

        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directory: " + this.uploadDir, e);
        }
    }

    @Override
    @Transactional
    public FileUploadResponse storeFile(MultipartFile file, String uploaderUsername) {
        if (file.isEmpty()) {
            throw new FileStorageException("Cannot upload an empty file");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new FileStorageException(
                    "File type not allowed. Allowed types: " + ALLOWED_MIME_TYPES);
        }

        User uploader = userRepository.findByUsername(uploaderUsername)
                .orElseThrow(() -> new ElementNotFoundException("User not found"));

        long currentTotalSize = fileMetadataRepository.sumSizeBytesByUploaderId(uploader.getId());
        long maxQuotaBytes = uploader.getRoles().stream()
                .mapToLong(role -> role.getFilesUploadQuotaBytes() != null ? role.getFilesUploadQuotaBytes() : 0L)
                .max()
                .orElse(0L);

        if (currentTotalSize + file.getSize() > maxQuotaBytes) {
            throw new FileStorageException(
                    "User upload quota exceeded (max " + formatSize(maxQuotaBytes) + "). Currently used: " +
                            formatSize(currentTotalSize));
        }

        String originalName = file.getOriginalFilename();
        String extension = extractExtension(originalName);
        UUID fileId = UUID.randomUUID();
        String storedName = fileId + extension;

        Path targetPath = uploadDir.resolve(storedName).normalize();
        if (!targetPath.startsWith(uploadDir)) {
            throw new FileStorageException("Cannot store file outside upload directory");
        }

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file: " + storedName, e);
        }

        FileMetadata metadata = FileMetadata.builder()
                .id(fileId)
                .originalName(originalName)
                .storedName(storedName)
                .contentType(contentType)
                .sizeBytes(file.getSize())
                .uploader(uploader)
                .createdAt(Instant.now())
                .build();

        fileMetadataRepository.save(metadata);

        log.info("File uploaded: {} -> {} by user {}", originalName, storedName, uploaderUsername);

        return FileUploadResponse.builder()
                .id(fileId)
                .originalName(originalName)
                .url(UPLOADS_URL_PREFIX + storedName)
                .size(file.getSize())
                .build();
    }

    @Override
    @Transactional
    public void deleteFile(UUID fileId) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ElementNotFoundException("File not found"));

        Path filePath = uploadDir.resolve(metadata.getStoredName()).normalize();

        try {
            Files.deleteIfExists(filePath);
            log.info("Deleted file from disk: {}", metadata.getStoredName());
        } catch (IOException e) {
            log.error("Failed to delete file from disk: {}", metadata.getStoredName(), e);
            throw new FileStorageException("Failed to delete file from disk", e);
        }

        fileMetadataRepository.delete(metadata);
        log.info("Deleted file metadata: {}", fileId);
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    private String formatSize(long sizeBytes) {
        if (sizeBytes >= 1024 * 1024) {
            return String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0));
        } else if (sizeBytes >= 1024) {
            return String.format("%.2f KB", sizeBytes / 1024.0);
        } else {
            return sizeBytes + " B";
        }
    }
}
