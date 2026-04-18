package org.nr31.backend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.dto.FileUploadResponse;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.exception.FileStorageException;
import org.nr31.backend.model.FileMetadata;
import org.nr31.backend.model.FileScope;
import org.nr31.backend.model.User;
import org.nr31.backend.repository.FileMetadataRepository;
import org.nr31.backend.repository.UserRepository;
import org.nr31.backend.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class LocalDriveStorageService implements FileStorageService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp");

    private static final String FILES_URL_PREFIX = "/api/v1/files/";

    private static final int PHYSICAL_PURGE_BATCH_SIZE = 500;

    private final Path uploadDir;
    private final FileMetadataRepository fileMetadataRepository;
    private final UserRepository userRepository;

    public LocalDriveStorageService(
            @Value("${app.uploads.dir:/app/uploads}") String uploadDir,
            FileMetadataRepository fileMetadataRepository,
            UserRepository userRepository,
            ObjectProvider<FileSystem> fileSystemProvider) {
        FileSystem fs = fileSystemProvider.getIfAvailable(FileSystems::getDefault);
        this.uploadDir = fs.getPath(uploadDir).toAbsolutePath().normalize();
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
    public FileUploadResponse storeFile(MultipartFile file, String uploaderUsername, FileScope scope) {
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

        String sha256Hash;
        Path tempFile;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            tempFile = Files.createTempFile(uploadDir, "upload-", ".tmp");

            try (InputStream inputStream = file.getInputStream();
                    DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
                Files.copy(digestInputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            sha256Hash = HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new FileStorageException("SHA-256 algorithm not available", e);
        } catch (IOException e) {
            throw new FileStorageException("Failed to process uploaded file", e);
        }

        Path targetPath = uploadDir.resolve(sha256Hash).normalize();
        if (!targetPath.startsWith(uploadDir)) {
            deleteTempFileSilently(tempFile);
            throw new FileStorageException("Cannot store file outside upload directory");
        }

        try {
            Files.move(tempFile, targetPath, StandardCopyOption.ATOMIC_MOVE);
        } catch (FileAlreadyExistsException e) {
            deleteTempFileSilently(tempFile);
            log.debug("CAS dedup: file with hash {} already exists on disk (caught during move)", sha256Hash);
        } catch (IOException e) {
            deleteTempFileSilently(tempFile);
            throw new FileStorageException("Failed to store file: " + sha256Hash, e);
        }

        String originalName = file.getOriginalFilename();
        UUID fileId = UUID.randomUUID();

        FileMetadata metadata = FileMetadata.builder()
                .id(fileId)
                .originalName(originalName)
                .storedName(sha256Hash)
                .contentType(contentType)
                .sizeBytes(file.getSize())
                .uploader(uploader)
                .createdAt(Instant.now())
                .scope(scope)
                .build();

        fileMetadataRepository.save(metadata);

        log.info("File uploaded: {} -> {} (hash: {}) by user {} [scope={}]",
                originalName, fileId, sha256Hash, uploaderUsername, scope);

        return FileUploadResponse.builder()
                .id(fileId)
                .originalName(originalName)
                .url(FILES_URL_PREFIX + fileId)
                .size(file.getSize())
                .build();
    }

    @Override
    @CacheEvict(value = "fileResolution", key = "#fileId")
    @Transactional
    public void deleteFile(UUID fileId) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ElementNotFoundException("File not found"));

        // Delete metadata only; physical file cleanup is deferred to the scheduled job
        fileMetadataRepository.delete(metadata);
        log.info("Deleted file metadata: {} (physical cleanup deferred to scheduled job)", fileId);
    }

    @Override
    @Cacheable("fileResolution")
    @Transactional(readOnly = true)
    public FileMetadata resolveFile(UUID fileId) {
        return fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ElementNotFoundException("File not found"));
    }

    @Override
    @Transactional
    public void purgeOrphanedAttachments(Instant threshold) {
        log.info("Starting metadata purge of orphaned attachments older than {}...", threshold);
        List<FileMetadata> orphans = fileMetadataRepository.findOrphanedAttachments(threshold);

        for (FileMetadata metadata : orphans) {
            fileMetadataRepository.delete(metadata);
            log.info("Purged orphaned attachment metadata: {} (hash: {})", metadata.getId(), metadata.getStoredName());
        }

        log.info("Metadata purge complete. {} orphaned attachments removed.", orphans.size());
    }

    @Override
    public void purgeOrphanedPhysicalFiles() {
        log.info("Starting physical purge of unreferenced files (batch size={})...", PHYSICAL_PURGE_BATCH_SIZE);
        int totalDeleted = 0;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(uploadDir)) {
            List<Path> batch = new ArrayList<>(PHYSICAL_PURGE_BATCH_SIZE);

            for (Path filePath : stream) {
                if (!Files.isRegularFile(filePath)) {
                    continue;
                }
                batch.add(filePath);

                if (batch.size() >= PHYSICAL_PURGE_BATCH_SIZE) {
                    totalDeleted += purgeBatch(batch);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                totalDeleted += purgeBatch(batch);
            }
        } catch (IOException e) {
            log.error("Error during physical file purge", e);
        }

        log.info("Physical purge complete. {} unreferenced files deleted.", totalDeleted);
    }

    private int purgeBatch(List<Path> batch) {
        List<String> fileNames = batch.stream()
                .map(p -> p.getFileName().toString())
                .toList();

        Set<String> referenced = fileMetadataRepository.findReferencedStoredNames(fileNames);
        int deleted = 0;

        for (Path filePath : batch) {
            String fileName = filePath.getFileName().toString();
            if (!referenced.contains(fileName)) {
                try {
                    Files.deleteIfExists(filePath);
                    deleted++;
                    log.info("Deleted unreferenced physical file: {}", fileName);
                } catch (IOException e) {
                    log.error("Failed to delete unreferenced physical file: {}", fileName, e);
                }
            }
        }

        return deleted;
    }

    private void deleteTempFileSilently(Path tempFile) {
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException ignored) {
            log.warn("Failed to delete temporary file: {}", tempFile);
        }
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
