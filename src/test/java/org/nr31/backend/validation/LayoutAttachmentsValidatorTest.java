package org.nr31.backend.validation;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nr31.backend.dto.cms.LayoutDataDto;
import org.nr31.backend.dto.cms.SlotDto;
import org.nr31.backend.dto.cms.RichTextWidgetDto;
import org.nr31.backend.model.FileMetadata;
import org.nr31.backend.model.FileScope;
import org.nr31.backend.model.User;
import org.nr31.backend.repository.FileMetadataRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LayoutAttachmentsValidatorTest {

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private LayoutAttachmentsValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new LayoutAttachmentsValidator(fileMetadataRepository);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void shouldReturnTrueWhenNoSlotsOrWidgets() {
        LayoutDataDto layoutData = new LayoutDataDto(null);
        boolean result = validator.isValid(layoutData, context);
        assertThat(result).isTrue();
        verifyNoInteractions(fileMetadataRepository);
    }

    @Test
    void shouldReturnTrueWhenNoAttachmentsInRichText() {
        RichTextWidgetDto widget = new RichTextWidgetDto();
        JsonNode body = objectMapper.readTree("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "paragraph",
                      "content": [
                        {"type": "text", "text": "Hello world without files"}
                      ]
                    }
                  ]
                }
                """);
        widget.setBodyContent(Map.of("en", body));

        SlotDto slot = new SlotDto("content", List.of(widget));
        LayoutDataDto layoutData = new LayoutDataDto(List.of(slot));

        boolean result = validator.isValid(layoutData, context);
        assertThat(result).isTrue();
        verifyNoInteractions(fileMetadataRepository);
    }

    @Test
    void shouldReturnFalseWhenAttachedFileDoesNotExist() {
        UUID missingFileId = UUID.randomUUID();
        RichTextWidgetDto widget = new RichTextWidgetDto();
        JsonNode body = objectMapper.readTree(String.format("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "fileAttachment",
                      "attrs": {
                        "id": "%s"
                      }
                    }
                  ]
                }
                """, missingFileId));
        widget.setBodyContent(Map.of("en", body));

        SlotDto slot = new SlotDto("content", List.of(widget));
        LayoutDataDto layoutData = new LayoutDataDto(List.of(slot));

        when(fileMetadataRepository.findAllById(any())).thenReturn(Collections.emptyList());
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

        boolean result = validator.isValid(layoutData, context);
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("cms_validation.layout.invalid_attachments");
    }

    @Test
    void shouldReturnTrueWhenNewlyAttachedFileIsOwnedByCurrentUser() {
        UUID fileId = UUID.randomUUID();
        RichTextWidgetDto widget = new RichTextWidgetDto();
        JsonNode body = objectMapper.readTree(String.format("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "fileAttachment",
                      "attrs": {
                        "id": "%s"
                      }
                    }
                  ]
                }
                """, fileId));
        widget.setBodyContent(Map.of("en", body));

        SlotDto slot = new SlotDto("content", List.of(widget));
        LayoutDataDto layoutData = new LayoutDataDto(List.of(slot));

        User currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("alice");
        FileMetadata metadata = FileMetadata.builder()
                .id(fileId)
                .scope(FileScope.ATTACHMENT)
                .uploader(currentUser)
                .originalName("test.png")
                .build();

        when(fileMetadataRepository.findAllById(any())).thenReturn(List.of(metadata));
        when(fileMetadataRepository.findReferencedAttachmentIds(any())).thenReturn(Collections.emptySet());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("alice");

        boolean result = validator.isValid(layoutData, context);
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenNewlyAttachedFileIsOwnedBySomeoneElse() {
        UUID fileId = UUID.randomUUID();
        RichTextWidgetDto widget = new RichTextWidgetDto();
        JsonNode body = objectMapper.readTree(String.format("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "fileAttachment",
                      "attrs": {
                        "id": "%s"
                      }
                    }
                  ]
                }
                """, fileId));
        widget.setBodyContent(Map.of("en", body));

        SlotDto slot = new SlotDto("content", List.of(widget));
        LayoutDataDto layoutData = new LayoutDataDto(List.of(slot));

        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("bob");
        FileMetadata metadata = FileMetadata.builder()
                .id(fileId)
                .scope(FileScope.ATTACHMENT)
                .uploader(otherUser)
                .originalName("bob_file.png")
                .build();

        when(fileMetadataRepository.findAllById(any())).thenReturn(List.of(metadata));
        when(fileMetadataRepository.findReferencedAttachmentIds(any())).thenReturn(Collections.emptySet());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("alice");
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

        boolean result = validator.isValid(layoutData, context);
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate("cms_validation.layout.unauthorized_attachment|fileName=bob_file.png");
    }

    @Test
    void shouldReturnTrueWhenAttachedFileIsOwnedBySomeoneElseButAlreadyReferencedInDatabase() {
        UUID fileId = UUID.randomUUID();
        RichTextWidgetDto widget = new RichTextWidgetDto();
        JsonNode body = objectMapper.readTree(String.format("""
                {
                  "type": "doc",
                  "content": [
                    {
                      "type": "fileAttachment",
                      "attrs": {
                        "id": "%s"
                      }
                    }
                  ]
                }
                """, fileId));
        widget.setBodyContent(Map.of("en", body));

        SlotDto slot = new SlotDto("content", List.of(widget));
        LayoutDataDto layoutData = new LayoutDataDto(List.of(slot));

        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setUsername("bob");
        FileMetadata metadata = FileMetadata.builder()
                .id(fileId)
                .scope(FileScope.ATTACHMENT)
                .uploader(otherUser)
                .originalName("bob_file.png")
                .build();

        when(fileMetadataRepository.findAllById(any())).thenReturn(List.of(metadata));
        // Simulate that this file is already referenced in page_revisions table
        when(fileMetadataRepository.findReferencedAttachmentIds(any())).thenReturn(Set.of(fileId));

        boolean result = validator.isValid(layoutData, context);
        assertThat(result).isTrue();
    }
}
