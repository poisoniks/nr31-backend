package org.nr31.backend.service;

import org.nr31.backend.dto.kb.CreateKbArticleRequest;
import org.nr31.backend.dto.kb.CreateKbFolderRequest;
import org.nr31.backend.dto.kb.KbArticleDetailDto;
import org.nr31.backend.dto.kb.KbFolderDetailDto;
import org.nr31.backend.dto.kb.KbFolderDto;
import org.nr31.backend.dto.kb.KbSearchResultDto;
import org.nr31.backend.dto.kb.UpdateKbArticleRequest;
import org.nr31.backend.dto.kb.UpdateKbFolderRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface KbService {

    // Folder CRUD
    List<KbFolderDto> getRootFolders();

    KbFolderDetailDto getFolderBySlug(String slug, Pageable pageable);

    KbFolderDto createFolder(CreateKbFolderRequest request);

    KbFolderDto updateFolder(Long id, UpdateKbFolderRequest request);

    void deleteFolder(Long id);

    // Article CRUD
    KbArticleDetailDto getArticleBySlug(String slug);

    KbArticleDetailDto createArticle(CreateKbArticleRequest request);

    KbArticleDetailDto updateArticle(Long articleId, UpdateKbArticleRequest request);

    void deleteArticle(Long articleId);

    // Search
    List<KbSearchResultDto> searchArticles(String query);
}
