package org.nr31.backend.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import net.jqwik.api.*;
import org.nr31.backend.dto.cms.*;
import org.nr31.backend.exception.ConflictException;
import org.nr31.backend.exception.ElementNotFoundException;
import org.nr31.backend.model.Page;
import org.nr31.backend.model.PageRevision;
import org.nr31.backend.model.RevisionStatus;
import org.nr31.backend.repository.PageRepository;
import org.nr31.backend.repository.PageRevisionRepository;
import org.nr31.backend.service.impl.CmsServiceImpl;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CmsServicePropertyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * For any update or publish operation where the provided version number does not 
     * match the current page version, the system SHALL reject the operation with 
     * HTTP 409 Conflict and include the current version in the error metadata.
     */
    @Property(tries = 100)
    @Label("Property 1: Optimistic Locking Enforcement - Stale versions are rejected")
    void staleVersionsAreRejectedWithConflict(
        @ForAll("validSlug") String slug,
        @ForAll("positiveInteger") Integer currentVersion,
        @ForAll("positiveInteger") Integer staleVersion
    ) {
        Assume.that(!currentVersion.equals(staleVersion));

        // Setup mocks
        PageRepository pageRepository = mock(PageRepository.class);
        PageRevisionRepository pageRevisionRepository = mock(PageRevisionRepository.class);
        ValidationService validationService = mock(ValidationService.class);
        DiscordWidgetService discordWidgetService = mock(DiscordWidgetService.class);
        YouTubeService youTubeService = mock(YouTubeService.class);
        CmsService cmsService = new CmsServiceImpl(pageRepository, pageRevisionRepository, validationService, discordWidgetService, youTubeService, objectMapper);

        // Setup page with current version
        Page page = Page.builder()
            .id(1L)
            .slug(slug)
            .title(Map.of("en", "Test Page"))
            .version(currentVersion)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        when(pageRepository.findBySlug(slug)).thenReturn(Optional.of(page));

        // Test updateDraft with stale version
        UpdateDraftRequest updateRequest = new UpdateDraftRequest(staleVersion, createSimpleLayout());
        
        assertThatThrownBy(() -> cmsService.updateDraft(slug, updateRequest))
            .isInstanceOf(ConflictException.class)
            .satisfies(exception -> {
                ConflictException conflict = (ConflictException) exception;
                Map<String, Object> metadata = conflict.getMetadata();
                
                assertThat(metadata)
                    .as("Conflict exception should include version metadata")
                    .containsEntry("currentVersion", currentVersion)
                    .containsEntry("providedVersion", staleVersion)
                    .containsEntry("slug", slug);
            });

        // Test publishDraft with stale version
        PublishDraftRequest publishRequest = new PublishDraftRequest(staleVersion);
        
        assertThatThrownBy(() -> cmsService.publishDraft(slug, publishRequest))
            .isInstanceOf(ConflictException.class)
            .satisfies(exception -> {
                ConflictException conflict = (ConflictException) exception;
                Map<String, Object> metadata = conflict.getMetadata();
                
                assertThat(metadata)
                    .as("Conflict exception should include version metadata")
                    .containsEntry("currentVersion", currentVersion)
                    .containsEntry("providedVersion", staleVersion)
                    .containsEntry("slug", slug);
            });
    }

    /**
     * For any successful draft publication with the correct version number, the 
     * system SHALL increment the page version by exactly 1 and return the new 
     * version in the response.
     */
    @Property(tries = 100)
    @Label("Property 2: Version Increment on Publication - Version increments by exactly 1")
    void versionIncrementsOnPublication(
        @ForAll("validSlug") String slug,
        @ForAll("positiveInteger") Integer currentVersion
    ) {
        // Setup mocks
        PageRepository pageRepository = mock(PageRepository.class);
        PageRevisionRepository pageRevisionRepository = mock(PageRevisionRepository.class);
        ValidationService validationService = mock(ValidationService.class);
        DiscordWidgetService discordWidgetService = mock(DiscordWidgetService.class);
        YouTubeService youTubeService = mock(YouTubeService.class);
        CmsService cmsService = new CmsServiceImpl(pageRepository, pageRevisionRepository, validationService, discordWidgetService, youTubeService, objectMapper);

        // Setup page with current version
        Page page = Page.builder()
            .id(1L)
            .slug(slug)
            .title(Map.of("en", "Test Page"))
            .version(currentVersion)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        LayoutDataDto layoutData = createSimpleLayout();
        com.fasterxml.jackson.databind.JsonNode layoutJson;
        try {
            layoutJson = new com.fasterxml.jackson.databind.ObjectMapper().readTree(objectMapper.writeValueAsString(layoutData));
        } catch(Exception e) { throw new RuntimeException(e); }

        PageRevision draftRevision = PageRevision.builder()
            .id(1L)
            .page(page)
            .layoutData(layoutJson)
            .status(RevisionStatus.DRAFT)
            .createdAt(Instant.now())
            .build();

        when(pageRepository.findBySlug(slug)).thenReturn(Optional.of(page));
        when(pageRevisionRepository.findByPageIdAndStatus(1L, RevisionStatus.DRAFT))
            .thenReturn(Optional.of(draftRevision));
        when(pageRevisionRepository.findByPageIdAndStatus(1L, RevisionStatus.PUBLISHED))
            .thenReturn(Optional.empty());
        when(pageRevisionRepository.save(any(PageRevision.class))).thenReturn(draftRevision);
        
        // Simulate version increment by @Version annotation
        Page savedPage = Page.builder()
            .id(1L)
            .slug(slug)
            .title(Map.of("en", "Test Page"))
            .version(currentVersion + 1)
            .createdAt(page.getCreatedAt())
            .updatedAt(Instant.now())
            .build();
        when(pageRepository.saveAndFlush(any(Page.class))).thenReturn(savedPage);
        when(pageRepository.findById(1L)).thenReturn(Optional.of(savedPage));

        // When: Publishing draft with correct version
        PublishDraftRequest request = new PublishDraftRequest(currentVersion);
        PageResponseDto response = cmsService.publishDraft(slug, request);

        // Then: Version should increment by exactly 1
        assertThat(response.getVersion())
            .as("Version should increment by exactly 1 after publication")
            .isEqualTo(currentVersion + 1);
    }


    /**
     * For any draft update with valid layout data and correct version number, the 
     * system SHALL update the draft revision without modifying the page version.
     */
    @Property(tries = 100)
    @Label("Property 3: Version Stability During Draft Updates - Version remains unchanged")
    void versionRemainsUnchangedDuringDraftUpdate(
        @ForAll("validSlug") String slug,
        @ForAll("positiveInteger") Integer version
    ) {
        // Setup mocks
        PageRepository pageRepository = mock(PageRepository.class);
        PageRevisionRepository pageRevisionRepository = mock(PageRevisionRepository.class);
        ValidationService validationService = mock(ValidationService.class);
        DiscordWidgetService discordWidgetService = mock(DiscordWidgetService.class);
        YouTubeService youTubeService = mock(YouTubeService.class);
        CmsService cmsService = new CmsServiceImpl(pageRepository, pageRevisionRepository, validationService, discordWidgetService, youTubeService, objectMapper);

        // Setup page with version
        Page page = Page.builder()
            .id(1L)
            .slug(slug)
            .title(Map.of("en", "Test Page"))
            .version(version)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        LayoutDataDto layoutData = createSimpleLayout();
        com.fasterxml.jackson.databind.JsonNode layoutJson;
        try {
            layoutJson = new com.fasterxml.jackson.databind.ObjectMapper().readTree(objectMapper.writeValueAsString(layoutData));
        } catch(Exception e) { throw new RuntimeException(e); }

        PageRevision draftRevision = PageRevision.builder()
            .id(1L)
            .page(page)
            .layoutData(layoutJson)
            .status(RevisionStatus.DRAFT)
            .createdAt(Instant.now())
            .build();

        when(pageRepository.findBySlug(slug)).thenReturn(Optional.of(page));
        when(pageRevisionRepository.findByPageIdAndStatus(1L, RevisionStatus.DRAFT))
            .thenReturn(Optional.of(draftRevision));
        when(pageRevisionRepository.save(any(PageRevision.class))).thenReturn(draftRevision);
        doNothing().when(validationService).validateLayout(any(LayoutDataDto.class));

        // When: Updating draft with correct version
        UpdateDraftRequest request = new UpdateDraftRequest(version, layoutData);
        PageResponseDto response = cmsService.updateDraft(slug, request);

        // Then: Version should remain unchanged
        assertThat(response.getVersion())
            .as("Version should remain unchanged during draft update")
            .isEqualTo(version);
    }

    /**
     * For any page with a PUBLISHED revision, retrieving the published page SHALL 
     * return HTTP 200 with complete data including slug, title, version, layout data, 
     * status, and creation timestamp.
     */
    @Property(tries = 100)
    @Label("Property 8: Published Page Retrieval Correctness - Returns complete data")
    void publishedPageRetrievalReturnsCompleteData(
        @ForAll("validSlug") String slug,
        @ForAll("validTitle") String title,
        @ForAll("positiveInteger") Integer version
    ) {
        // Setup mocks
        PageRepository pageRepository = mock(PageRepository.class);
        PageRevisionRepository pageRevisionRepository = mock(PageRevisionRepository.class);
        ValidationService validationService = mock(ValidationService.class);
        DiscordWidgetService discordWidgetService = mock(DiscordWidgetService.class);
        YouTubeService youTubeService = mock(YouTubeService.class);
        CmsService cmsService = new CmsServiceImpl(pageRepository, pageRevisionRepository, validationService, discordWidgetService, youTubeService, objectMapper);

        // Setup page with published revision
        Map<String, String> localizedTitle = Map.of("en", title);
        Page page = Page.builder()
            .id(1L)
            .slug(slug)
            .title(localizedTitle)
            .version(version)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        LayoutDataDto layoutData = createSimpleLayout();
        com.fasterxml.jackson.databind.JsonNode layoutJson;
        try {
            layoutJson = new com.fasterxml.jackson.databind.ObjectMapper().readTree(objectMapper.writeValueAsString(layoutData));
        } catch(Exception e) { throw new RuntimeException(e); }

        PageRevision publishedRevision = PageRevision.builder()
            .id(1L)
            .page(page)
            .layoutData(layoutJson)
            .status(RevisionStatus.PUBLISHED)
            .createdAt(Instant.now())
            .build();

        when(pageRevisionRepository.findByPageSlugAndStatusWithPage(slug, RevisionStatus.PUBLISHED))
            .thenReturn(Optional.of(publishedRevision));

        // When: Retrieving published page
        PageResponseDto response = cmsService.getPublishedPage(slug);

        // Then: Should return complete data
        assertThat(response)
            .as("Response should contain complete page data")
            .isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getSlug()).isEqualTo(slug);
        assertThat(response.getTitle()).isEqualTo(localizedTitle);
        assertThat(response.getVersion()).isEqualTo(version);
        assertThat(response.getLayoutData()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(RevisionStatus.PUBLISHED);
        assertThat(response.getCreatedAt()).isNotNull();
    }

    /**
     * For any page with only DRAFT or ARCHIVED revisions (no PUBLISHED revision), 
     * attempting to retrieve the published page SHALL return HTTP 404 Not Found.
     */
    @Property(tries = 100)
    @Label("Property 9: Published-Only Public Access - Non-published pages return 404")
    void nonPublishedPagesReturn404(
        @ForAll("validSlug") String slug
    ) {
        // Setup mocks
        PageRepository pageRepository = mock(PageRepository.class);
        PageRevisionRepository pageRevisionRepository = mock(PageRevisionRepository.class);
        ValidationService validationService = mock(ValidationService.class);
        DiscordWidgetService discordWidgetService = mock(DiscordWidgetService.class);
        YouTubeService youTubeService = mock(YouTubeService.class);
        CmsService cmsService = new CmsServiceImpl(pageRepository, pageRevisionRepository, validationService, discordWidgetService, youTubeService, objectMapper);

        // Setup: No published revision exists
        when(pageRevisionRepository.findByPageSlugAndStatusWithPage(slug, RevisionStatus.PUBLISHED))
            .thenReturn(Optional.empty());

        // When/Then: Retrieving published page should throw ElementNotFoundException
        assertThatThrownBy(() -> cmsService.getPublishedPage(slug))
            .isInstanceOf(ElementNotFoundException.class)
            .satisfies(exception -> {
                ElementNotFoundException notFound = (ElementNotFoundException) exception;
                Map<String, Object> metadata = notFound.getMetadata();
                
                assertThat(metadata)
                    .as("Exception should include slug in metadata")
                    .containsEntry("slug", slug);
            });
    }

    /**
     * For any page with a PUBLISHED revision but no DRAFT revision, requesting the 
     * draft SHALL create a new DRAFT revision with the same layout data as the 
     * published revision.
     */
    @Property(tries = 100)
    @Label("Property 10: Draft Creation from Published - Creates draft with same layout")
    void draftCreatedFromPublishedWithSameLayout(
        @ForAll("validSlug") String slug,
        @ForAll("validTitle") String title,
        @ForAll("positiveInteger") Integer version
    ) {
        // Setup mocks
        PageRepository pageRepository = mock(PageRepository.class);
        PageRevisionRepository pageRevisionRepository = mock(PageRevisionRepository.class);
        ValidationService validationService = mock(ValidationService.class);
        DiscordWidgetService discordWidgetService = mock(DiscordWidgetService.class);
        YouTubeService youTubeService = mock(YouTubeService.class);
        CmsService cmsService = new CmsServiceImpl(pageRepository, pageRevisionRepository, validationService, discordWidgetService, youTubeService, objectMapper);

        // Setup page with published revision but no draft
        Page page = Page.builder()
            .id(1L)
            .slug(slug)
            .title(Map.of("en", title))
            .version(version)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        LayoutDataDto layoutData = createSimpleLayout();
        com.fasterxml.jackson.databind.JsonNode layoutJson;
        try {
            layoutJson = new com.fasterxml.jackson.databind.ObjectMapper().readTree(objectMapper.writeValueAsString(layoutData));
        } catch(Exception e) { throw new RuntimeException(e); }

        PageRevision publishedRevision = PageRevision.builder()
            .id(1L)
            .page(page)
            .layoutData(layoutJson)
            .status(RevisionStatus.PUBLISHED)
            .createdAt(Instant.now())
            .build();

        PageRevision newDraftRevision = PageRevision.builder()
            .id(2L)
            .page(page)
            .layoutData(layoutJson)
            .status(RevisionStatus.DRAFT)
            .createdAt(Instant.now())
            .build();

        when(pageRevisionRepository.findByPageSlugAndStatusWithPage(slug, RevisionStatus.DRAFT))
            .thenReturn(Optional.empty());
        when(pageRevisionRepository.findByPageSlugAndStatusWithPage(slug, RevisionStatus.PUBLISHED))
            .thenReturn(Optional.of(publishedRevision));
        when(pageRevisionRepository.save(any(PageRevision.class))).thenReturn(newDraftRevision);

        // When: Requesting draft page
        PageResponseDto response = cmsService.getDraftPage(slug);

        // Then: Should create new draft with same layout
        assertThat(response.getStatus())
            .as("New revision should have DRAFT status")
            .isEqualTo(RevisionStatus.DRAFT);
        assertThat(response.getLayoutData())
            .as("New draft should have same layout as published")
            .isNotNull();
        
        // Verify that save was called to create new draft
        verify(pageRevisionRepository).save(argThat(revision -> 
            revision.getStatus() == RevisionStatus.DRAFT &&
            revision.getLayoutData().equals(layoutJson)
        ));
    }


    /**
     * For any page with a DRAFT revision, retrieving the draft SHALL return HTTP 200 
     * with complete data including the current page version number and complete layout data.
     */
    @Property(tries = 100)
    @Label("Property 11: Draft Retrieval Completeness - Returns complete draft data")
    void draftRetrievalReturnsCompleteData(
        @ForAll("validSlug") String slug,
        @ForAll("validTitle") String title,
        @ForAll("positiveInteger") Integer version
    ) {
        // Setup mocks
        PageRepository pageRepository = mock(PageRepository.class);
        PageRevisionRepository pageRevisionRepository = mock(PageRevisionRepository.class);
        ValidationService validationService = mock(ValidationService.class);
        DiscordWidgetService discordWidgetService = mock(DiscordWidgetService.class);
        YouTubeService youTubeService = mock(YouTubeService.class);
        CmsService cmsService = new CmsServiceImpl(pageRepository, pageRevisionRepository, validationService, discordWidgetService, youTubeService, objectMapper);

        // Setup page with draft revision
        Map<String, String> localizedTitle = Map.of("en", title);
        Page page = Page.builder()
            .id(1L)
            .slug(slug)
            .title(localizedTitle)
            .version(version)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        LayoutDataDto layoutData = createSimpleLayout();
        com.fasterxml.jackson.databind.JsonNode layoutJson;
        try {
            layoutJson = new com.fasterxml.jackson.databind.ObjectMapper().readTree(objectMapper.writeValueAsString(layoutData));
        } catch(Exception e) { throw new RuntimeException(e); }

        PageRevision draftRevision = PageRevision.builder()
            .id(1L)
            .page(page)
            .layoutData(layoutJson)
            .status(RevisionStatus.DRAFT)
            .createdAt(Instant.now())
            .build();

        when(pageRevisionRepository.findByPageSlugAndStatusWithPage(slug, RevisionStatus.DRAFT))
            .thenReturn(Optional.of(draftRevision));

        // When: Retrieving draft page
        PageResponseDto response = cmsService.getDraftPage(slug);

        // Then: Should return complete data with current version
        assertThat(response)
            .as("Response should contain complete draft data")
            .isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getSlug()).isEqualTo(slug);
        assertThat(response.getTitle()).isEqualTo(localizedTitle);
        assertThat(response.getVersion())
            .as("Response should include current page version")
            .isEqualTo(version);
        assertThat(response.getLayoutData())
            .as("Response should include complete layout data")
            .isNotNull();
        assertThat(response.getStatus()).isEqualTo(RevisionStatus.DRAFT);
        assertThat(response.getCreatedAt()).isNotNull();
    }

    /**
     * For any successful draft publication, the system SHALL change the revision 
     * status from DRAFT to PUBLISHED.
     */
    @Property(tries = 100)
    @Label("Property 12: Status Transition on Publication - Status changes to PUBLISHED")
    void statusChangesToPublishedOnPublication(
        @ForAll("validSlug") String slug,
        @ForAll("positiveInteger") Integer version
    ) {
        // Setup mocks
        PageRepository pageRepository = mock(PageRepository.class);
        PageRevisionRepository pageRevisionRepository = mock(PageRevisionRepository.class);
        ValidationService validationService = mock(ValidationService.class);
        DiscordWidgetService discordWidgetService = mock(DiscordWidgetService.class);
        YouTubeService youTubeService = mock(YouTubeService.class);
        CmsService cmsService = new CmsServiceImpl(pageRepository, pageRevisionRepository, validationService, discordWidgetService, youTubeService, objectMapper);

        // Setup page with draft revision
        Page page = Page.builder()
            .id(1L)
            .slug(slug)
            .title(Map.of("en", "Test Page"))
            .version(version)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        LayoutDataDto layoutData = createSimpleLayout();
        com.fasterxml.jackson.databind.JsonNode layoutJson;
        try {
            layoutJson = new com.fasterxml.jackson.databind.ObjectMapper().readTree(objectMapper.writeValueAsString(layoutData));
        } catch(Exception e) { throw new RuntimeException(e); }

        PageRevision draftRevision = PageRevision.builder()
            .id(1L)
            .page(page)
            .layoutData(layoutJson)
            .status(RevisionStatus.DRAFT)
            .createdAt(Instant.now())
            .build();

        when(pageRepository.findBySlug(slug)).thenReturn(Optional.of(page));
        when(pageRevisionRepository.findByPageIdAndStatus(1L, RevisionStatus.DRAFT))
            .thenReturn(Optional.of(draftRevision));
        when(pageRevisionRepository.findByPageIdAndStatus(1L, RevisionStatus.PUBLISHED))
            .thenReturn(Optional.empty());
        when(pageRevisionRepository.save(any(PageRevision.class))).thenAnswer(invocation -> invocation.<PageRevision>getArgument(0));
        when(pageRepository.saveAndFlush(any(Page.class))).thenReturn(page);
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));

        // When: Publishing draft
        PublishDraftRequest request = new PublishDraftRequest(version);
        PageResponseDto response = cmsService.publishDraft(slug, request);

        // Then: Status should be PUBLISHED
        assertThat(response.getStatus())
            .as("Status should change from DRAFT to PUBLISHED")
            .isEqualTo(RevisionStatus.PUBLISHED);
        
        // Verify that the draft revision status was changed to PUBLISHED
        verify(pageRevisionRepository).save(argThat(revision -> 
            revision.getStatus() == RevisionStatus.PUBLISHED
        ));
    }

    /**
     * For any page with both DRAFT and PUBLISHED revisions, publishing the draft 
     * SHALL change the old PUBLISHED revision status to ARCHIVED.
     */
    @Property(tries = 100)
    @Label("Property 13: Previous Published Archival - Old published becomes archived")
    void oldPublishedBecomesArchivedOnPublication(
        @ForAll("validSlug") String slug,
        @ForAll("positiveInteger") Integer version
    ) {
        // Setup mocks
        PageRepository pageRepository = mock(PageRepository.class);
        PageRevisionRepository pageRevisionRepository = mock(PageRevisionRepository.class);
        ValidationService validationService = mock(ValidationService.class);
        DiscordWidgetService discordWidgetService = mock(DiscordWidgetService.class);
        YouTubeService youTubeService = mock(YouTubeService.class);
        CmsService cmsService = new CmsServiceImpl(pageRepository, pageRevisionRepository, validationService, discordWidgetService, youTubeService, objectMapper);

        // Setup page with both draft and published revisions
        Page page = Page.builder()
            .id(1L)
            .slug(slug)
            .title(Map.of("en", "Test Page"))
            .version(version)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        LayoutDataDto layoutData = createSimpleLayout();
        com.fasterxml.jackson.databind.JsonNode layoutJson;
        try {
            layoutJson = new com.fasterxml.jackson.databind.ObjectMapper().readTree(objectMapper.writeValueAsString(layoutData));
        } catch(Exception e) { throw new RuntimeException(e); }

        PageRevision draftRevision = PageRevision.builder()
            .id(1L)
            .page(page)
            .layoutData(layoutJson)
            .status(RevisionStatus.DRAFT)
            .createdAt(Instant.now())
            .build();

        PageRevision publishedRevision = PageRevision.builder()
            .id(2L)
            .page(page)
            .layoutData(layoutJson)
            .status(RevisionStatus.PUBLISHED)
            .createdAt(Instant.now().minusSeconds(3600))
            .build();

        when(pageRepository.findBySlug(slug)).thenReturn(Optional.of(page));
        when(pageRevisionRepository.findByPageIdAndStatus(1L, RevisionStatus.DRAFT))
            .thenReturn(Optional.of(draftRevision));
        when(pageRevisionRepository.findByPageIdAndStatus(1L, RevisionStatus.PUBLISHED))
            .thenReturn(Optional.of(publishedRevision));
        when(pageRevisionRepository.save(any(PageRevision.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pageRepository.saveAndFlush(any(Page.class))).thenReturn(page);
        when(pageRepository.findById(1L)).thenReturn(Optional.of(page));

        // When: Publishing draft
        PublishDraftRequest request = new PublishDraftRequest(version);
        cmsService.publishDraft(slug, request);

        // Then: Old published revision should be archived
        verify(pageRevisionRepository, times(2)).save(any(PageRevision.class));
        verify(pageRevisionRepository).save(argThat(revision -> 
            revision.getId().equals(2L) && revision.getStatus() == RevisionStatus.ARCHIVED
        ));
    }

    /**
     * For any request to retrieve a page with a slug that does not exist, the system 
     * SHALL return HTTP 404 Not Found with a descriptive error message and include 
     * the requested slug in the error metadata.
     */
    @Property(tries = 100)
    @Label("Property 14: Non-Existent Page Error Response - Returns 404 with slug in metadata")
    void nonExistentPageReturns404WithSlugInMetadata(
        @ForAll("nonExistentSlug") String slug
    ) {
        // Setup mocks
        PageRepository pageRepository = mock(PageRepository.class);
        PageRevisionRepository pageRevisionRepository = mock(PageRevisionRepository.class);
        ValidationService validationService = mock(ValidationService.class);
        DiscordWidgetService discordWidgetService = mock(DiscordWidgetService.class);
        YouTubeService youTubeService = mock(YouTubeService.class);
        CmsService cmsService = new CmsServiceImpl(pageRepository, pageRevisionRepository, validationService, discordWidgetService, youTubeService, objectMapper);

        // Setup: No page or revision exists for this slug
        when(pageRevisionRepository.findByPageSlugAndStatusWithPage(slug, RevisionStatus.PUBLISHED))
            .thenReturn(Optional.empty());
        when(pageRevisionRepository.findByPageSlugAndStatusWithPage(slug, RevisionStatus.DRAFT))
            .thenReturn(Optional.empty());
        when(pageRepository.findBySlug(slug))
            .thenReturn(Optional.empty());

        // Test getPublishedPage with non-existent slug
        assertThatThrownBy(() -> cmsService.getPublishedPage(slug))
            .isInstanceOf(ElementNotFoundException.class)
            .satisfies(exception -> {
                ElementNotFoundException notFound = (ElementNotFoundException) exception;
                Map<String, Object> metadata = notFound.getMetadata();
                
                assertThat(notFound.getMessage())
                    .as("Exception should have descriptive error message")
                    .isNotBlank()
                    .containsIgnoringCase("not found");
                
                assertThat(metadata)
                    .as("Exception should include slug in metadata")
                    .containsEntry("slug", slug);
            });

        // Test getDraftPage with non-existent slug
        assertThatThrownBy(() -> cmsService.getDraftPage(slug))
            .isInstanceOf(ElementNotFoundException.class)
            .satisfies(exception -> {
                ElementNotFoundException notFound = (ElementNotFoundException) exception;
                Map<String, Object> metadata = notFound.getMetadata();
                
                assertThat(notFound.getMessage())
                    .as("Exception should have descriptive error message")
                    .isNotBlank()
                    .containsIgnoringCase("not found");
                
                assertThat(metadata)
                    .as("Exception should include slug in metadata")
                    .containsEntry("slug", slug);
            });
    }

    @Provide
    Arbitrary<String> validSlug() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .withChars('-')
            .ofMinLength(3)
            .ofMaxLength(50)
            .filter(s -> !s.startsWith("-") && !s.endsWith("-"));
    }

    @Provide
    Arbitrary<String> nonExistentSlug() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .withChars('-', '_')
            .ofMinLength(5)
            .ofMaxLength(100)
            .map(s -> "nonexistent-" + s);
    }

    @Provide
    Arbitrary<String> validTitle() {
        return Arbitraries.strings()
            .alpha()
            .numeric()
            .withChars(' ', '-', ':', '!')
            .ofMinLength(3)
            .ofMaxLength(100);
    }

    @Provide
    Arbitrary<Integer> positiveInteger() {
        return Arbitraries.integers().between(1, 1000);
    }

    private LayoutDataDto createSimpleLayout() {
        RichTextWidgetDto richTextWidget = new RichTextWidgetDto();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode content = mapper.readTree("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Sample content\"}]}]}");
            richTextWidget.setBodyContent(Map.of("en", content));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create test layout", e);
        }

        SlotDto slot = new SlotDto("content", List.of(richTextWidget));
        return new LayoutDataDto(List.of(slot));
    }
}
