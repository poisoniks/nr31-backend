package org.nr31.backend.service;

import org.nr31.backend.dto.cms.PageResponseDto;
import org.nr31.backend.dto.cms.PublishDraftRequest;
import org.nr31.backend.dto.cms.UpdateDraftRequest;

/**
 * Service interface for CMS operations managing pages and revisions.
 * Provides core functionality for retrieving, updating, and publishing page content.
 */
public interface CmsService {
    
    /**
     * Retrieves the published revision of a page by slug.
     * 
     * @param slug the URL-friendly page identifier
     * @return the published page data
     * @throws org.nr31.backend.exception.ElementNotFoundException if no published revision exists
     */
    PageResponseDto getPublishedPage(String slug);
    
    /**
     * Retrieves the draft revision of a page by slug.
     * If no draft exists, duplicates the published revision as a new draft.
     * 
     * @param slug the URL-friendly page identifier
     * @return the draft page data
     * @throws org.nr31.backend.exception.ElementNotFoundException if page doesn't exist
     */
    PageResponseDto getDraftPage(String slug);
    
    /**
     * Updates the draft revision with new layout data.
     * Validates version for optimistic locking and layout against slot restrictions.
     * 
     * @param slug the URL-friendly page identifier
     * @param request the update request containing version and layout data
     * @return the updated draft page data
     * @throws org.nr31.backend.exception.ConflictException if version is stale
     * @throws jakarta.validation.ValidationException if layout validation fails
     */
    PageResponseDto updateDraft(String slug, UpdateDraftRequest request);
    
    /**
     * Publishes a draft revision, archiving the current published revision if it exists.
     * Increments the page version number.
     * 
     * @param slug the URL-friendly page identifier
     * @param request the publish request containing version
     * @return the newly published page data
     * @throws org.nr31.backend.exception.ConflictException if version is stale
     * @throws org.nr31.backend.exception.ElementNotFoundException if no draft exists
     */
    PageResponseDto publishDraft(String slug, PublishDraftRequest request);
}
