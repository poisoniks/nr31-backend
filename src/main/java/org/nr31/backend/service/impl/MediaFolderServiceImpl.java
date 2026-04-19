package org.nr31.backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.dto.MediaFolderDTO;
import org.nr31.backend.dto.MediaFolderRequest;
import org.nr31.backend.exception.ConflictException;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.model.MediaFolder;
import org.nr31.backend.repository.FileMetadataRepository;
import org.nr31.backend.repository.MediaFolderRepository;
import org.nr31.backend.service.MediaFolderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaFolderServiceImpl implements MediaFolderService {

    private final MediaFolderRepository mediaFolderRepository;
    private final FileMetadataRepository fileMetadataRepository;

    @Override
    @Transactional
    public MediaFolderDTO createFolder(MediaFolderRequest request) {
        MediaFolder parent = resolveParent(request.getParentId());

        MediaFolder folder = MediaFolder.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .parent(parent)
                .createdAt(Instant.now())
                .build();

        mediaFolderRepository.save(folder);
        log.info("Created media folder '{}' (id={}, parentId={})",
                folder.getName(), folder.getId(), request.getParentId());

        return toDTO(folder);
    }

    @Override
    @Transactional
    public MediaFolderDTO updateFolder(UUID id, MediaFolderRequest request) {
        MediaFolder folder = mediaFolderRepository.findByIdWithParent(id)
                .orElseThrow(() -> new ElementNotFoundException("Folder not found"));

        MediaFolder parent = resolveParent(request.getParentId());

        folder.setName(request.getName());
        folder.setParent(parent);

        mediaFolderRepository.save(folder);
        log.info("Updated media folder '{}' (id={}, parentId={})",
                folder.getName(), folder.getId(), request.getParentId());

        return toDTO(folder);
    }

    @Override
    @Transactional
    public void deleteFolder(UUID id) {
        MediaFolder folder = mediaFolderRepository.findById(id)
                .orElseThrow(() -> new ElementNotFoundException("Folder not found"));

        if (mediaFolderRepository.existsByParentId(id)) {
            throw new ConflictException("Cannot delete folder: it contains sub-folders");
        }

        if (fileMetadataRepository.existsByFolder(folder)) {
            throw new ConflictException("Cannot delete folder: it contains files");
        }

        mediaFolderRepository.delete(folder);
        log.info("Deleted media folder id={}", id);
    }

    private MediaFolder resolveParent(UUID parentId) {
        if (parentId == null) {
            return null;
        }

        return mediaFolderRepository.findById(parentId)
                .orElseThrow(() -> new ElementNotFoundException("Parent folder not found"));
    }

    private MediaFolderDTO toDTO(MediaFolder folder) {
        return MediaFolderDTO.builder()
                .id(folder.getId())
                .name(folder.getName())
                .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
                .createdAt(folder.getCreatedAt())
                .build();
    }
}
