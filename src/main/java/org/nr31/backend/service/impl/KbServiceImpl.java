package org.nr31.backend.service.impl;

import tools.jackson.databind.JsonNode;
import com.github.slugify.Slugify;
import org.nr31.backend.dto.ErrorCode;
import org.nr31.backend.dto.kb.CreateKbArticleRequest;
import org.nr31.backend.dto.kb.CreateKbFolderRequest;
import org.nr31.backend.dto.kb.KbArticleDetailDto;
import org.nr31.backend.dto.kb.KbArticleSummaryDto;
import org.nr31.backend.dto.kb.KbFolderDetailDto;
import org.nr31.backend.dto.kb.KbFolderDto;
import org.nr31.backend.dto.kb.KbSearchResultDto;
import org.nr31.backend.dto.kb.UpdateKbArticleRequest;
import org.nr31.backend.dto.kb.UpdateKbFolderRequest;
import org.nr31.backend.exception.ConflictException;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.model.KbArticle;
import org.nr31.backend.model.KbFolder;
import org.nr31.backend.model.User;
import org.nr31.backend.repository.KbArticleRepository;
import org.nr31.backend.repository.KbFolderRepository;
import org.nr31.backend.repository.UserRepository;
import org.nr31.backend.dto.AppConfigDto;
import org.nr31.backend.model.AppConfigKey;
import org.nr31.backend.service.AppConfigService;
import org.nr31.backend.service.KbService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class KbServiceImpl implements KbService {

    private final KbFolderRepository kbFolderRepository;
    private final KbArticleRepository kbArticleRepository;
    private final UserRepository userRepository;
    private final AppConfigService appConfigService;
    private final Slugify slugify;

    public KbServiceImpl(
            KbFolderRepository kbFolderRepository,
            KbArticleRepository kbArticleRepository,
            UserRepository userRepository,
            AppConfigService appConfigService) {
        this.kbFolderRepository = kbFolderRepository;
        this.kbArticleRepository = kbArticleRepository;
        this.userRepository = userRepository;
        this.appConfigService = appConfigService;
        this.slugify = Slugify.builder().build();
    }

    // ==========================================
    // Folder CRUD
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<KbFolderDto> getRootFolders() {
        return kbFolderRepository.findByParentIsNullOrderByName().stream()
                .map(this::mapToFolderDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public KbFolderDetailDto getFolderBySlug(String slug, Pageable pageable) {
        KbFolder folder = kbFolderRepository.findBySlugAndParentIsNull(slug)
                .or(() -> kbFolderRepository.findFirstBySlug(slug))
                .orElseThrow(() -> new ElementNotFoundException("Folder not found with slug: " + slug, ErrorCode.KB_FOLDER_NOT_FOUND));

        List<KbFolderDto> subFolders = kbFolderRepository.findByParentIdOrderByName(folder.getId()).stream()
                .map(this::mapToFolderDto)
                .toList();

        Page<KbArticleSummaryDto> articles = kbArticleRepository.findByFolderIdOrderByCreatedAtDesc(folder.getId(), pageable)
                .map(this::mapToArticleSummaryDto);

        return KbFolderDetailDto.builder()
                .id(folder.getId())
                .name(folder.getName())
                .slug(folder.getSlug())
                .restricted(folder.isRestricted())
                .subFolders(subFolders)
                .articles(articles)
                .build();
    }

    @Override
    @Transactional
    public KbFolderDto createFolder(CreateKbFolderRequest request) {
        validateAdminPrivileges();

        KbFolder parent = null;
        if (request.getParentId() != null) {
            parent = kbFolderRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ElementNotFoundException("Parent folder not found", ErrorCode.KB_FOLDER_NOT_FOUND));
        }

        String slug = generateUniqueFolderSlug(request.getName(), request.getParentId(), null);

        KbFolder folder = KbFolder.builder()
                .name(request.getName())
                .slug(slug)
                .parent(parent)
                .restricted(Boolean.TRUE.equals(request.getRestricted()))
                .build();

        KbFolder saved = kbFolderRepository.save(folder);
        return mapToFolderDto(saved);
    }

    @Override
    @Transactional
    public KbFolderDto updateFolder(Long id, UpdateKbFolderRequest request) {
        validateAdminPrivileges();

        KbFolder folder = kbFolderRepository.findById(id)
                .orElseThrow(() -> new ElementNotFoundException("Folder not found", ErrorCode.KB_FOLDER_NOT_FOUND));

        if (request.getParentId() != null) {
            if (request.getParentId() == -1) {
                folder.setParent(null);
                // Regenerate slug at root level
                folder.setSlug(generateUniqueFolderSlug(request.getName() != null ? request.getName() : folder.getName(), null, id));
            } else {
                if (request.getParentId().equals(id)) {
                    throw new ConflictException("A folder cannot be its own parent", ErrorCode.CONFLICT);
                }
                KbFolder newParent = kbFolderRepository.findById(request.getParentId())
                        .orElseThrow(() -> new ElementNotFoundException("Parent folder not found", ErrorCode.KB_FOLDER_NOT_FOUND));

                // Verify circular hierarchy
                KbFolder walk = newParent;
                while (walk != null) {
                    if (walk.getId().equals(id)) {
                        throw new ConflictException("Circular parent relationship detected", ErrorCode.CONFLICT);
                    }
                    walk = walk.getParent();
                }

                folder.setParent(newParent);
                // Regenerate slug under new parent
                folder.setSlug(generateUniqueFolderSlug(request.getName() != null ? request.getName() : folder.getName(), newParent.getId(), id));
            }
        } else if (request.getName() != null) {
            // Name change under same parent
            folder.setSlug(generateUniqueFolderSlug(request.getName(), folder.getParent() != null ? folder.getParent().getId() : null, id));
        }

        if (request.getName() != null) {
            folder.setName(request.getName());
        }

        if (request.getRestricted() != null) {
            folder.setRestricted(request.getRestricted());
        }

        KbFolder saved = kbFolderRepository.save(folder);
        return mapToFolderDto(saved);
    }

    @Override
    @Transactional
    public void deleteFolder(Long id) {
        validateAdminPrivileges();

        KbFolder folder = kbFolderRepository.findById(id)
                .orElseThrow(() -> new ElementNotFoundException("Folder not found", ErrorCode.KB_FOLDER_NOT_FOUND));

        if (kbFolderRepository.existsByParentId(id)) {
            throw new ConflictException("Folder has sub-folders and cannot be deleted", ErrorCode.KB_FOLDER_NOT_EMPTY, Map.of("id", id));
        }
        if (kbArticleRepository.existsByFolderId(id)) {
            throw new ConflictException("Folder has articles and cannot be deleted", ErrorCode.KB_FOLDER_NOT_EMPTY, Map.of("id", id));
        }

        kbFolderRepository.delete(folder);
    }

    // ==========================================
    // Article CRUD
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public KbArticleDetailDto getArticleBySlug(String slug) {
        KbArticle article = kbArticleRepository.findBySlug(slug)
                .orElseThrow(() -> new ElementNotFoundException("Article not found with slug: " + slug, ErrorCode.KB_ARTICLE_NOT_FOUND));

        return mapToArticleDetailDto(article);
    }

    @Override
    @Transactional
    public KbArticleDetailDto createArticle(CreateKbArticleRequest request) {
        KbFolder folder = kbFolderRepository.findById(request.getFolderId())
                .orElseThrow(() -> new ElementNotFoundException("Folder not found with ID: " + request.getFolderId(), ErrorCode.KB_FOLDER_NOT_FOUND));

        validateFolderWriteAccess(folder);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User author = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new ElementNotFoundException("User not found: " + auth.getName(), ErrorCode.USER_NOT_FOUND));

        String baseSlug = slugify.slugify(getSlugSource(request.getTitle()));
        if (baseSlug == null || baseSlug.isEmpty()) {
            baseSlug = "article";
        }
        String uniqueSlug = generateUniqueArticleSlug(request.getTitle());
        KbArticle article = KbArticle.builder()
                .folder(folder)
                .author(author)
                .title(request.getTitle())
                .slug(uniqueSlug)
                .content(request.getContent())
                .build();

        KbArticle saved = saveArticleWithSlugRetry(article, baseSlug);
        return mapToArticleDetailDto(saved);
    }

    @Override
    @Transactional
    public KbArticleDetailDto updateArticle(Long articleId, UpdateKbArticleRequest request) {
        KbArticle article = kbArticleRepository.findById(articleId)
                .orElseThrow(() -> new ElementNotFoundException("Article not found with ID: " + articleId, ErrorCode.KB_ARTICLE_NOT_FOUND));

        validateArticleModifyAccess(article);

        if (request.getFolderId() != null) {
            KbFolder newFolder = kbFolderRepository.findById(request.getFolderId())
                    .orElseThrow(() -> new ElementNotFoundException("Folder not found with ID: " + request.getFolderId(), ErrorCode.KB_FOLDER_NOT_FOUND));

            validateFolderWriteAccess(newFolder);
            article.setFolder(newFolder);
        }

        String baseSlug = article.getSlug();
        if (request.getTitle() != null && !request.getTitle().isEmpty() && !request.getTitle().equals(article.getTitle())) {
            article.setTitle(request.getTitle());
            baseSlug = slugify.slugify(getSlugSource(request.getTitle()));
            if (baseSlug == null || baseSlug.isEmpty()) {
                baseSlug = "article";
            }
            String uniqueSlug = generateUniqueArticleSlug(request.getTitle());
            article.setSlug(uniqueSlug);
        }

        if (request.getContent() != null) {
            article.setContent(request.getContent());
        }

        KbArticle saved = saveArticleWithSlugRetry(article, baseSlug);
        return mapToArticleDetailDto(saved);
    }

    @Override
    @Transactional
    public void deleteArticle(Long articleId) {
        KbArticle article = kbArticleRepository.findById(articleId)
                .orElseThrow(() -> new ElementNotFoundException("Article not found with ID: " + articleId, ErrorCode.KB_ARTICLE_NOT_FOUND));

        validateArticleModifyAccess(article);

        kbArticleRepository.delete(article);
    }

    // ==========================================
    // Search
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public List<KbSearchResultDto> searchArticles(String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        String trimmedQuery = query.trim();
        String precision = getSearchPrecision();

        List<KbArticle> results = switch (precision) {
            case "standard" -> kbArticleRepository.searchStandard(trimmedQuery);
            case "full" -> kbArticleRepository.searchFull(trimmedQuery);
            default -> kbArticleRepository.searchBasic(trimmedQuery);
        };

        return results.stream()
                .map(article -> KbSearchResultDto.builder()
                        .article(mapToArticleSummaryDto(article))
                        .breadcrumbs(generateBreadcrumbs(article.getFolder().getId()))
                        .build())
                .toList();
    }

    private String getSearchPrecision() {
        try {
            AppConfigDto config = appConfigService.getConfig(AppConfigKey.KB_SEARCH_PRECISION);
            JsonNode node = config.getConfigValue();
            String value = (node != null) ? node.asString("") : "";
            return switch (value) {
                case "basic", "standard", "full" -> value;
                default -> "full";
            };
        } catch (Exception e) {
            return "full";
        }
    }

    // ==========================================
    // Private Helpers
    // ==========================================

    private void validateAdminPrivileges() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !hasAuthority(auth, "kb:admin")) {
            throw new AccessDeniedException("Admin privileges required.");
        }
    }

    private void validateFolderWriteAccess(KbFolder folder) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }

        boolean isAdmin = hasAuthority(auth, "kb:admin");
        boolean isWriter = hasAuthority(auth, "kb:write");

        if (!isAdmin && !isWriter) {
            throw new AccessDeniedException("Write privileges required.");
        }

        if (folder.isRestricted() && !isAdmin) {
            throw new AccessDeniedException("Admin privileges required for this restricted folder.");
        }
    }

    private void validateArticleModifyAccess(KbArticle article) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }

        boolean isAdmin = hasAuthority(auth, "kb:admin");
        boolean isWriter = hasAuthority(auth, "kb:write");

        if (!isAdmin) {
            if (!isWriter) {
                throw new AccessDeniedException("Write privileges required.");
            }
            if (!article.getAuthor().getUsername().equals(auth.getName())) {
                throw new AccessDeniedException("Only the author or an admin can modify this article.");
            }
        }
    }

    private boolean hasAuthority(Authentication auth, String authorityName) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> authorityName.equals(a.getAuthority()));
    }

    private String generateUniqueFolderSlug(Map<String, String> name, Long parentId, Long excludeId) {
        String baseSlug = slugify.slugify(getSlugSource(name));
        if (baseSlug == null || baseSlug.isEmpty()) {
            baseSlug = "folder";
        }
        String uniqueSlug = baseSlug;
        int counter = 1;
        while (folderExists(uniqueSlug, parentId, excludeId)) {
            uniqueSlug = baseSlug + "-" + counter++;
        }
        return uniqueSlug;
    }

    private boolean folderExists(String slug, Long parentId, Long excludeId) {
        if (excludeId != null) {
            if (parentId == null) {
                return kbFolderRepository.existsBySlugAndParentIsNullAndIdNot(slug, excludeId);
            } else {
                return kbFolderRepository.existsBySlugAndParentIdAndIdNot(slug, parentId, excludeId);
            }
        } else {
            if (parentId == null) {
                return kbFolderRepository.existsBySlugAndParentIsNull(slug);
            } else {
                return kbFolderRepository.existsBySlugAndParentId(slug, parentId);
            }
        }
    }

    private String generateUniqueArticleSlug(Map<String, String> title) {
        String baseSlug = slugify.slugify(getSlugSource(title));
        if (baseSlug == null || baseSlug.isEmpty()) {
            baseSlug = "article";
        }
        String uniqueSlug = baseSlug;
        int counter = 1;
        while (kbArticleRepository.existsBySlug(uniqueSlug)) {
            uniqueSlug = baseSlug + "-" + counter++;
        }
        return uniqueSlug;
    }

    private String getSlugSource(Map<String, String> localizedMap) {
        if (localizedMap == null || localizedMap.isEmpty()) {
            return "";
        }
        if (localizedMap.containsKey("en")) {
            return localizedMap.get("en");
        }
        return localizedMap.values().iterator().next();
    }

    private KbArticle saveArticleWithSlugRetry(KbArticle article, String baseSlug) {
        int attempts = 0;
        String uniqueSlug = article.getSlug();
        while (attempts < 5) {
            try {
                article.setSlug(uniqueSlug);
                return kbArticleRepository.saveAndFlush(article);
            } catch (DataIntegrityViolationException e) {
                attempts++;
                uniqueSlug = baseSlug + "-" + UUID.randomUUID().toString().substring(0, 8);
            }
        }
        throw new ConflictException("Failed to generate a unique slug for the article due to concurrency.", ErrorCode.CONFLICT);
    }


    private List<KbFolderDto> generateBreadcrumbs(Long folderId) {
        if (folderId == null) {
            return List.of();
        }
        List<KbFolder> ancestors = kbFolderRepository.findAncestorChain(folderId);
        return ancestors.stream()
                .map(this::mapToFolderDto)
                .toList();
    }

    private KbFolderDto mapToFolderDto(KbFolder folder) {
        return KbFolderDto.builder()
                .id(folder.getId())
                .name(folder.getName())
                .slug(folder.getSlug())
                .restricted(folder.isRestricted())
                .build();
    }

    private KbArticleSummaryDto mapToArticleSummaryDto(KbArticle article) {
        return KbArticleSummaryDto.builder()
                .id(article.getId())
                .title(article.getTitle())
                .slug(article.getSlug())
                .authorId(article.getAuthor().getId())
                .authorName(article.getAuthor().getUsername())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }

    private KbArticleDetailDto mapToArticleDetailDto(KbArticle article) {
        return KbArticleDetailDto.builder()
                .id(article.getId())
                .folderId(article.getFolder().getId())
                .folderName(article.getFolder().getName())
                .authorId(article.getAuthor().getId())
                .authorName(article.getAuthor().getUsername())
                .title(article.getTitle())
                .slug(article.getSlug())
                .content(article.getContent())
                .breadcrumbs(generateBreadcrumbs(article.getFolder().getId()))
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }



}
