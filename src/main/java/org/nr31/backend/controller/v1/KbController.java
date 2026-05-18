package org.nr31.backend.controller.v1;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.kb.CreateKbArticleRequest;
import org.nr31.backend.dto.kb.CreateKbFolderRequest;
import org.nr31.backend.dto.kb.KbArticleDetailDto;
import org.nr31.backend.dto.kb.KbFolderDetailDto;
import org.nr31.backend.dto.kb.KbFolderDto;
import org.nr31.backend.dto.kb.KbSearchResultDto;
import org.nr31.backend.dto.kb.UpdateKbArticleRequest;
import org.nr31.backend.dto.kb.UpdateKbFolderRequest;
import org.nr31.backend.service.KbService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/kb")
@RequiredArgsConstructor
@Tag(name = "Knowledge Base", description = "Endpoints for managing folders and articles in the Knowledge Base")
public class KbController {

    private final KbService kbService;

    // ==========================================
    // Folder Endpoints
    // ==========================================

    @Operation(summary = "Get top-level (root) folders")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of root folders retrieved successfully")
    })
    @GetMapping("/folders/root")
    public ResponseEntity<List<KbFolderDto>> getRootFolders() {
        return ResponseEntity.ok(kbService.getRootFolders());
    }

    @Operation(summary = "Get folder details by slug")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Folder details, subfolders, and articles retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Folder not found")
    })
    @GetMapping("/folders/{slug}")
    public ResponseEntity<KbFolderDetailDto> getFolderBySlug(
            @PathVariable String slug,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(kbService.getFolderBySlug(slug, pageable));
    }

    @Operation(summary = "Create a new folder")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Folder created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PostMapping("/folders")
    @PreAuthorize("hasAuthority('kb:admin')")
    public ResponseEntity<KbFolderDto> createFolder(@Valid @RequestBody CreateKbFolderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(kbService.createFolder(request));
    }

    @Operation(summary = "Update an existing folder")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Folder updated successfully"),
            @ApiResponse(responseCode = "404", description = "Folder not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PutMapping("/folders/{id}")
    @PreAuthorize("hasAuthority('kb:admin')")
    public ResponseEntity<KbFolderDto> updateFolder(
            @PathVariable Long id,
            @Valid @RequestBody UpdateKbFolderRequest request) {
        return ResponseEntity.ok(kbService.updateFolder(id, request));
    }

    @Operation(summary = "Delete an empty folder")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Folder deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Folder is not empty"),
            @ApiResponse(responseCode = "404", description = "Folder not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @DeleteMapping("/folders/{id}")
    @PreAuthorize("hasAuthority('kb:admin')")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long id) {
        kbService.deleteFolder(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // Article Endpoints
    // ==========================================

    @Operation(summary = "Get an article by slug")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Article details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Article not found")
    })
    @GetMapping("/articles/{slug}")
    public ResponseEntity<KbArticleDetailDto> getArticleBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(kbService.getArticleBySlug(slug));
    }

    @Operation(summary = "Create a new article")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Article created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PostMapping("/articles")
    @PreAuthorize("hasAnyAuthority('kb:write', 'kb:admin')")
    public ResponseEntity<KbArticleDetailDto> createArticle(@Valid @RequestBody CreateKbArticleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(kbService.createArticle(request));
    }

    @Operation(summary = "Update an existing article")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Article updated successfully"),
            @ApiResponse(responseCode = "404", description = "Article not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @PutMapping("/articles/{id}")
    @PreAuthorize("hasAuthority('kb:admin') or (@kbSecurity.isAuthor(authentication, #id) and hasAuthority('kb:write'))")
    public ResponseEntity<KbArticleDetailDto> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody UpdateKbArticleRequest request) {
        return ResponseEntity.ok(kbService.updateArticle(id, request));
    }

    @Operation(summary = "Delete an article")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Article deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Article not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    @DeleteMapping("/articles/{id}")
    @PreAuthorize("hasAuthority('kb:admin') or (@kbSecurity.isAuthor(authentication, #id) and hasAuthority('kb:write'))")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long id) {
        kbService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // Search Endpoints
    // ==========================================

    @Operation(summary = "Search articles via full-text search and trigrams")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Search completed successfully")
    })
    @GetMapping("/search")
    public ResponseEntity<List<KbSearchResultDto>> searchArticles(@RequestParam("q") String query) {
        return ResponseEntity.ok(kbService.searchArticles(query));
    }
}
