package org.nr31.backend.service;

import org.nr31.backend.dto.MediaFolderDTO;
import org.nr31.backend.dto.MediaFolderRequest;

import java.util.UUID;

public interface MediaFolderService {

    MediaFolderDTO createFolder(MediaFolderRequest request);

    MediaFolderDTO updateFolder(UUID id, MediaFolderRequest request);

    void deleteFolder(UUID id);
}
