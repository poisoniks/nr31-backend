package org.nr31.backend.service.impl;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.ErrorCode;
import org.nr31.backend.dto.cms.LayoutDataDto;
import org.nr31.backend.dto.cms.PageResponseDto;
import org.nr31.backend.dto.cms.PublishDraftRequest;
import org.nr31.backend.dto.cms.UpdateDraftRequest;
import org.nr31.backend.exception.ConflictException;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.model.Page;
import org.nr31.backend.model.PageRevision;
import org.nr31.backend.model.RevisionStatus;
import org.nr31.backend.repository.PageRepository;
import org.nr31.backend.repository.PageRevisionRepository;
import org.nr31.backend.service.CmsService;
import org.nr31.backend.service.ValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CmsServiceImpl implements CmsService {

    private final PageRepository pageRepository;
    private final PageRevisionRepository pageRevisionRepository;
    private final ValidationService validationService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponseDto getPublishedPage(String slug) {
        PageRevision publishedRevision = pageRevisionRepository
                .findByPageSlugAndStatusWithPage(slug, RevisionStatus.PUBLISHED)
                .orElseThrow(() -> new ElementNotFoundException(
                        "Published page not found",
                        ErrorCode.ELEMENT_NOT_FOUND,
                        Map.of("slug", slug)
                ));

        LayoutDataDto layoutData = deserializeLayoutData(publishedRevision.getLayoutData());
        return mapToResponseDto(publishedRevision.getPage(), publishedRevision, layoutData);
    }

    @Override
    @Transactional
    public PageResponseDto getDraftPage(String slug) {
        PageRevision draftRevision = pageRevisionRepository
                .findByPageSlugAndStatusWithPage(slug, RevisionStatus.DRAFT)
                .orElse(null);

        if (draftRevision != null) {
            LayoutDataDto layoutData = deserializeLayoutData(draftRevision.getLayoutData());
            return mapToResponseDto(draftRevision.getPage(), draftRevision, layoutData);
        }

        PageRevision publishedRevision = pageRevisionRepository
                .findByPageSlugAndStatusWithPage(slug, RevisionStatus.PUBLISHED)
                .orElseThrow(() -> new ElementNotFoundException(
                        "Page not found",
                        ErrorCode.ELEMENT_NOT_FOUND,
                        Map.of("slug", slug)
                ));

        PageRevision newDraft = PageRevision.builder()
                .page(publishedRevision.getPage())
                .layoutData(publishedRevision.getLayoutData())
                .status(RevisionStatus.DRAFT)
                .build();

        PageRevision savedDraft = pageRevisionRepository.save(newDraft);
        LayoutDataDto layoutData = deserializeLayoutData(savedDraft.getLayoutData());
        return mapToResponseDto(savedDraft.getPage(), savedDraft, layoutData);
    }

    @Override
    @Transactional
    public PageResponseDto updateDraft(String slug, UpdateDraftRequest request) {
        Page page = pageRepository.findBySlug(slug)
                .orElseThrow(() -> new ElementNotFoundException(
                        "Page not found",
                        ErrorCode.ELEMENT_NOT_FOUND,
                        Map.of("slug", slug)
                ));

        if (!page.getVersion().equals(request.getVersion())) {
            throw new ConflictException(
                    "Version conflict: page was modified by another user",
                    ErrorCode.CONFLICT,
                    Map.of(
                            "providedVersion", request.getVersion(),
                            "currentVersion", page.getVersion(),
                            "slug", slug
                    )
            );
        }

        validationService.validateLayout(request.getLayoutData());

        PageRevision draftRevision = pageRevisionRepository
                .findByPageIdAndStatus(page.getId(), RevisionStatus.DRAFT)
                .orElseThrow(() -> new ElementNotFoundException(
                        "Draft revision not found",
                        ErrorCode.ELEMENT_NOT_FOUND,
                        Map.of("slug", slug, "status", "DRAFT")
                ));

        JsonNode layoutDataJson = serializeLayoutData(request.getLayoutData());
        draftRevision.setLayoutData(layoutDataJson);

        PageRevision savedRevision = pageRevisionRepository.save(draftRevision);
        return mapToResponseDto(page, savedRevision, request.getLayoutData());
    }

    @Override
    @Transactional
    public PageResponseDto publishDraft(String slug, PublishDraftRequest request) {
        Page page = pageRepository.findBySlug(slug)
                .orElseThrow(() -> new ElementNotFoundException(
                        "Page not found",
                        ErrorCode.ELEMENT_NOT_FOUND,
                        Map.of("slug", slug)
                ));

        if (!page.getVersion().equals(request.getVersion())) {
            throw new ConflictException(
                    "Version conflict: page was modified by another user",
                    ErrorCode.CONFLICT,
                    Map.of(
                            "providedVersion", request.getVersion(),
                            "currentVersion", page.getVersion(),
                            "slug", slug
                    )
            );
        }

        PageRevision draftRevision = pageRevisionRepository
                .findByPageIdAndStatus(page.getId(), RevisionStatus.DRAFT)
                .orElseThrow(() -> new ElementNotFoundException(
                        "Draft revision not found",
                        ErrorCode.ELEMENT_NOT_FOUND,
                        Map.of("slug", slug, "status", "DRAFT")
                ));

        pageRevisionRepository.findByPageIdAndStatus(page.getId(), RevisionStatus.PUBLISHED)
                .ifPresent(published -> {
                    published.setStatus(RevisionStatus.ARCHIVED);
                    pageRevisionRepository.save(published);
                });

        draftRevision.setStatus(RevisionStatus.PUBLISHED);
        PageRevision savedRevision = pageRevisionRepository.save(draftRevision);

        // Save page to trigger version increment via @Version
        // The version will be incremented automatically by JPA when the entity is modified
        page.setUpdatedAt(Instant.now());
        Page savedPage = pageRepository.saveAndFlush(page);
        
        // Refresh to get the updated version from the database
        pageRepository.flush();
        Page refreshedPage = pageRepository.findById(savedPage.getId()).orElse(savedPage);

        LayoutDataDto layoutData = deserializeLayoutData(savedRevision.getLayoutData());
        return mapToResponseDto(refreshedPage, savedRevision, layoutData);
    }

    private static final com.fasterxml.jackson.databind.ObjectMapper legacyMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    private LayoutDataDto deserializeLayoutData(JsonNode layoutDataJson) {
        try {
            return objectMapper.readValue(layoutDataJson.toString(), LayoutDataDto.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to deserialize layout data", e);
        }
    }

    private JsonNode serializeLayoutData(LayoutDataDto layoutData) {
        try {
            String jsonString = objectMapper.writeValueAsString(layoutData);
            return legacyMapper.readTree(jsonString);
        } catch (JacksonException | com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize layout data", e);
        }
    }

    private PageResponseDto mapToResponseDto(Page page, PageRevision revision, LayoutDataDto layoutData) {
        return new PageResponseDto(
                page.getId(),
                page.getSlug(),
                page.getTitle(),
                page.getVersion(),
                layoutData,
                revision.getStatus(),
                OffsetDateTime.ofInstant(revision.getCreatedAt(), ZoneOffset.UTC)
        );
    }
}
