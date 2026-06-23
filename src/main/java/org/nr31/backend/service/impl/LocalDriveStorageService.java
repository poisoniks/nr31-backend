package org.nr31.backend.service.impl;

import tools.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.dto.admin.AppConfigDto;
import org.nr31.backend.dto.common.ErrorCode;
import org.nr31.backend.dto.media.FileMetadataDTO;
import org.nr31.backend.dto.media.FileUploadResponse;
import org.nr31.backend.dto.media.LibraryFileUpdateRequest;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.exception.FileStorageException;
import org.nr31.backend.model.AppConfigKey;
import org.nr31.backend.model.FileMetadata;
import org.nr31.backend.model.FileScope;
import org.nr31.backend.model.MediaFolder;
import org.nr31.backend.model.User;
import org.nr31.backend.repository.FileMetadataRepository;
import org.nr31.backend.repository.MediaFolderRepository;
import org.nr31.backend.repository.UserRepository;
import org.nr31.backend.service.AppConfigService;
import org.nr31.backend.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class LocalDriveStorageService implements FileStorageService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/webp",
            "application/pdf",
            "application/zip",
            "text/plain",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel");

    private static final String FILES_URL_PREFIX = "/api/v1/files/";

    private static final int PHYSICAL_PURGE_BATCH_SIZE = 500;

    private final Path uploadDir;
    private final FileMetadataRepository fileMetadataRepository;
    private final UserRepository userRepository;
    private final MediaFolderRepository mediaFolderRepository;
    private final AppConfigService appConfigService;

    public LocalDriveStorageService(
            @Value("${app.uploads.dir:/app/uploads}") String uploadDir,
            FileMetadataRepository fileMetadataRepository,
            UserRepository userRepository,
            MediaFolderRepository mediaFolderRepository,
            ObjectProvider<FileSystem> fileSystemProvider,
            AppConfigService appConfigService) {
        FileSystem fs = fileSystemProvider.getIfAvailable(FileSystems::getDefault);
        this.uploadDir = fs.getPath(uploadDir).toAbsolutePath().normalize();
        this.fileMetadataRepository = fileMetadataRepository;
        this.userRepository = userRepository;
        this.mediaFolderRepository = mediaFolderRepository;
        this.appConfigService = appConfigService;

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
            throw new FileStorageException("Cannot upload an empty file", ErrorCode.EMPTY_FILE);
        }

        String contentType = file.getContentType();
        Set<String> allowedMimeTypes = getAllowedMimeTypes();
        if (contentType == null || !allowedMimeTypes.contains(contentType)) {
            throw new FileStorageException(
                    "File type not allowed. Allowed types: " + allowedMimeTypes, ErrorCode.INVALID_FILE_TYPE);
        }

        User uploader = userRepository.findByUsername(uploaderUsername)
                .orElseThrow(() -> new ElementNotFoundException("User not found", ErrorCode.USER_NOT_FOUND, Map.of("username", uploaderUsername)));

        long currentTotalSize = fileMetadataRepository.sumSizeBytesByUploaderId(uploader.getId());
        long maxQuotaBytes = uploader.getRoles().stream()
                .mapToLong(role -> role.getFilesUploadQuotaBytes() != null ? role.getFilesUploadQuotaBytes() : 0L)
                .max()
                .orElse(0L);

        if (currentTotalSize + file.getSize() > maxQuotaBytes) {
            throw new FileStorageException(
                    "User upload quota exceeded (max " + formatSize(maxQuotaBytes) + "). Currently used: " +
                            formatSize(currentTotalSize),
                    ErrorCode.QUOTA_EXCEEDED,
                    Map.of("currentSize", currentTotalSize, "maxQuota", maxQuotaBytes, "fileSize", file.getSize()));
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

        try {
            return finalizeStorage(sha256Hash, uploader, file.getOriginalFilename(), contentType, file.getSize(), scope, targetPath -> {
                try {
                    Files.move(tempFile, targetPath, StandardCopyOption.ATOMIC_MOVE);
                } catch (FileAlreadyExistsException e) {
                    log.debug("CAS dedup: file with hash {} already exists on disk (caught during move)", sha256Hash);
                } catch (IOException e) {
                    throw new FileStorageException("Failed to store file: " + sha256Hash, e);
                }
            });
        } finally {
            deleteTempFileSilently(tempFile);
        }
    }

    @Override
    @Transactional
    public FileUploadResponse storeFile(byte[] bytes, String originalName, String contentType, String uploaderUsername, FileScope scope) {
        if (bytes == null || bytes.length == 0) {
            throw new FileStorageException("Cannot store an empty file", ErrorCode.EMPTY_FILE);
        }

        User uploader = userRepository.findByUsername(uploaderUsername)
                .orElseThrow(() -> new ElementNotFoundException("User not found", ErrorCode.USER_NOT_FOUND, Map.of("username", uploaderUsername)));

        String sha256Hash;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(bytes);
            sha256Hash = HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new FileStorageException("SHA-256 algorithm not available", e);
        }

        return finalizeStorage(sha256Hash, uploader, originalName, contentType, (long) bytes.length, scope, targetPath -> {
            try {
                Files.write(targetPath, bytes);
            } catch (IOException e) {
                throw new FileStorageException("Failed to store file: " + sha256Hash, e);
            }
        });
    }

    @Override
    @CacheEvict(value = "fileResolution", key = "#fileId")
    @Transactional
    public void deleteFile(UUID fileId) {
        FileMetadata metadata = fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ElementNotFoundException("File not found", ErrorCode.FILE_NOT_FOUND, Map.of("id", fileId)));

        // Delete metadata only; physical file cleanup is deferred to the scheduled job
        fileMetadataRepository.delete(metadata);
        log.info("Deleted file metadata: {} (physical cleanup deferred to scheduled job)", fileId);
    }

    @Override
    @Cacheable("fileResolution")
    @Transactional(readOnly = true)
    public FileMetadata resolveFile(UUID fileId) {
        return fileMetadataRepository.findById(fileId)
                .orElseThrow(() -> new ElementNotFoundException("File not found", ErrorCode.FILE_NOT_FOUND, Map.of("id", fileId)));
    }

    @Override
    @Transactional
    public FileUploadResponse storeLibraryFile(MultipartFile file, String uploaderUsername, UUID folderId) {
        MediaFolder folder = null;
        if (folderId != null) {
            folder = mediaFolderRepository.findById(folderId)
                    .orElseThrow(() -> new ElementNotFoundException("Folder not found", ErrorCode.FOLDER_NOT_FOUND, Map.of("id", folderId)));
        }

        FileUploadResponse response = storeFile(file, uploaderUsername, FileScope.LIBRARY);

        if (folder != null) {
            FileMetadata metadata = fileMetadataRepository.findById(response.getId())
                    .orElseThrow(() -> new ElementNotFoundException("File not found after upload", ErrorCode.FILE_NOT_FOUND, Map.of("id", response.getId())));
            metadata.setFolder(folder);
            fileMetadataRepository.save(metadata);
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileMetadataDTO> listLibraryFiles(UUID folderId, Pageable pageable) {
        Page<FileMetadata> page;
        if (folderId != null) {
            MediaFolder folder = mediaFolderRepository.findById(folderId)
                    .orElseThrow(() -> new ElementNotFoundException("Folder not found", ErrorCode.FOLDER_NOT_FOUND, Map.of("id", folderId)));
            page = fileMetadataRepository.findByScopeAndFolder(FileScope.LIBRARY, folder, pageable);
        } else {
            page = fileMetadataRepository.findByScopeAndFolderIsNull(FileScope.LIBRARY, pageable);
        }
        return page.map(this::toFileMetadataDTO);
    }

    @Override
    @Transactional
    public FileMetadataDTO updateLibraryFile(UUID id, LibraryFileUpdateRequest request) {
        FileMetadata metadata = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new ElementNotFoundException("File not found", ErrorCode.FILE_NOT_FOUND, Map.of("id", id)));

        if (request.getName() != null) {
            metadata.setOriginalName(request.getName());
        }

        if (request.getFolderId() != null) {
            MediaFolder folder = mediaFolderRepository.findById(request.getFolderId())
                    .orElseThrow(() -> new ElementNotFoundException("Target folder not found", ErrorCode.FOLDER_NOT_FOUND, Map.of("id", request.getFolderId())));
            metadata.setFolder(folder);
        } else {
            metadata.setFolder(null);
        }

        fileMetadataRepository.save(metadata);
        log.info("Updated library file metadata: {} (name={}, folder={})",
                id, metadata.getOriginalName(),
                metadata.getFolder() != null ? metadata.getFolder().getId() : "root");

        return toFileMetadataDTO(metadata);
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

    private FileUploadResponse finalizeStorage(String sha256Hash, User uploader, String originalName, String contentType, long sizeBytes, FileScope scope, java.util.function.Consumer<Path> fileWriter) {
        if (scope == FileScope.ATTACHMENT) {
            Optional<FileMetadata> existingMetadata = fileMetadataRepository
                    .findByStoredNameAndUploaderIdAndScope(sha256Hash, uploader.getId(), scope);
            if (existingMetadata.isPresent()) {
                FileMetadata metadata = existingMetadata.get();
                metadata.setCreatedAt(Instant.now());
                fileMetadataRepository.save(metadata);
                log.info("Deduplicated file upload (hash: {}) for user {}. Reset GC clock.", sha256Hash, uploader.getUsername());
                return FileUploadResponse.builder()
                        .id(metadata.getId())
                        .originalName(metadata.getOriginalName())
                        .url(FILES_URL_PREFIX + metadata.getId())
                        .size(metadata.getSizeBytes())
                        .build();
            }
        }

        Path targetPath = uploadDir.resolve(sha256Hash).normalize();
        if (!targetPath.startsWith(uploadDir)) {
            throw new FileStorageException("Cannot store file outside upload directory");
        }

        fileWriter.accept(targetPath);

        UUID fileId = UUID.randomUUID();

        FileMetadata metadata = FileMetadata.builder()
                .id(fileId)
                .originalName(originalName)
                .storedName(sha256Hash)
                .contentType(contentType)
                .sizeBytes(sizeBytes)
                .uploader(uploader)
                .createdAt(Instant.now())
                .scope(scope)
                .build();

        fileMetadataRepository.save(metadata);

        log.info("File uploaded: {} -> {} (hash: {}) by user {} [scope={}]",
                originalName, fileId, sha256Hash, uploader.getUsername(), scope);

        return FileUploadResponse.builder()
                .id(fileId)
                .originalName(originalName)
                .url(FILES_URL_PREFIX + fileId)
                .size(sizeBytes)
                .build();
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

    private FileMetadataDTO toFileMetadataDTO(FileMetadata metadata) {
        return FileMetadataDTO.builder()
                .id(metadata.getId())
                .name(metadata.getOriginalName())
                .url(FILES_URL_PREFIX + metadata.getId())
                .contentType(metadata.getContentType())
                .sizeBytes(metadata.getSizeBytes())
                .folderId(metadata.getFolder() != null ? metadata.getFolder().getId() : null)
                .uploaderUsername(metadata.getUploader().getUsername())
                .createdAt(metadata.getCreatedAt())
                .build();
    }

    @Override
    public Set<String> getAllowedMimeTypes() {
        try {
            AppConfigDto config = appConfigService.getConfig(AppConfigKey.ALLOWED_MIME_TYPES);
            JsonNode node = config.getConfigValue();
            if (node != null && node.isArray()) {
                Set<String> allowedMimeTypes = new HashSet<>();
                for (JsonNode item : node) {
                    allowedMimeTypes.add(item.asString());
                }
                return allowedMimeTypes;
            }
        } catch (Exception e) {
            log.error("Failed to load allowed MIME types from database app_config, falling back to defaults", e);
        }
        return ALLOWED_MIME_TYPES;
    }
}
