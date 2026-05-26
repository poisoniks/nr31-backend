package org.nr31.backend.service;

import org.nr31.backend.dto.media.MediaFolderDTO;
import org.nr31.backend.dto.media.MediaFolderRequest;

import java.util.List;
import java.util.UUID;

public interface MediaFolderService {

    MediaFolderDTO createFolder(MediaFolderRequest request);

    MediaFolderDTO updateFolder(UUID id, MediaFolderRequest request);

    void deleteFolder(UUID id);

    List<MediaFolderDTO> listFolders(UUID parentId);
}
