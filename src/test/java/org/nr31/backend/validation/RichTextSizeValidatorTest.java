package org.nr31.backend.validation;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nr31.backend.dto.AppConfigDto;
import org.nr31.backend.model.AppConfigKey;
import org.nr31.backend.service.AppConfigService;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RichTextSizeValidatorTest {

    @Mock
    private AppConfigService appConfigService;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    private RichTextSizeValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new RichTextSizeValidator(appConfigService, objectMapper);
    }

    @Test
    void shouldReturnTrueForNullValue() {
        boolean result = validator.isValid(null, context);
        assertThat(result).isTrue();
        verifyNoInteractions(appConfigService);
    }

    @Test
    void shouldReturnTrueForEmptyMap() {
        Map<String, JsonNode> emptyMap = new HashMap<>();
        boolean result = validator.isValid(emptyMap, context);
        assertThat(result).isTrue();
        verifyNoInteractions(appConfigService);
    }

    @Test
    void shouldReturnTrueWhenContentIsWithinSizeLimit() throws Exception {
        // Setup: max size of 1000 bytes
        AppConfigDto config = AppConfigDto.builder()
            .name("cms.richtext.max_size_bytes")
            .configValue(objectMapper.readTree("1000"))
            .build();
        when(appConfigService.getConfig(AppConfigKey.CMS_RICHTEXT_MAX_SIZE_BYTES)).thenReturn(config);

        // Create small content (well under 1000 bytes)
        JsonNode smallContent = objectMapper.readTree("""
            {
              "type": "doc",
              "content": [
                {
                  "type": "paragraph",
                  "content": [
                    {"type": "text", "text": "Small content"}
                  ]
                }
              ]
            }
            """);

        Map<String, JsonNode> bodyContent = Map.of("en", smallContent);

        boolean result = validator.isValid(bodyContent, context);
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalseWhenContentExceedsSizeLimit() throws Exception {
        // Setup: max size of 100 bytes (very small)
        AppConfigDto config = AppConfigDto.builder()
            .name("cms.richtext.max_size_bytes")
            .configValue(objectMapper.readTree("100"))
            .build();
        when(appConfigService.getConfig(AppConfigKey.CMS_RICHTEXT_MAX_SIZE_BYTES)).thenReturn(config);

        // Create large content (over 100 bytes)
        JsonNode largeContent = objectMapper.readTree("""
            {
              "type": "doc",
              "content": [
                {
                  "type": "paragraph",
                  "content": [
                    {"type": "text", "text": "This is a very long text that will definitely exceed 100 bytes when serialized to JSON format with all the structure"}
                  ]
                }
              ]
            }
            """);

        Map<String, JsonNode> bodyContent = Map.of("en", largeContent);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

        boolean result = validator.isValid(bodyContent, context);
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(contains("cms_validation.richtext.size_exceeded"));
    }

    @Test
    void shouldValidateEachLocaleIndependently() throws Exception {
        // Setup: max size of 200 bytes
        AppConfigDto config = AppConfigDto.builder()
            .name("cms.richtext.max_size_bytes")
            .configValue(objectMapper.readTree("200"))
            .build();
        when(appConfigService.getConfig(AppConfigKey.CMS_RICHTEXT_MAX_SIZE_BYTES)).thenReturn(config);

        // Create content where one locale is within limit, another exceeds it
        JsonNode smallContent = objectMapper.readTree("""
            {
              "type": "doc",
              "content": [
                {
                  "type": "paragraph",
                  "content": [
                    {"type": "text", "text": "Small"}
                  ]
                }
              ]
            }
            """);

        JsonNode largeContent = objectMapper.readTree("""
            {
              "type": "doc",
              "content": [
                {
                  "type": "paragraph",
                  "content": [
                    {"type": "text", "text": "This is a very long text that will definitely exceed 200 bytes when serialized to JSON format with all the structure and formatting"}
                  ]
                }
              ]
            }
            """);

        Map<String, JsonNode> bodyContent = Map.of(
            "en", smallContent,
            "uk", largeContent
        );

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

        boolean result = validator.isValid(bodyContent, context);
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(contains("uk"));
    }

    @Test
    void shouldUseDefaultSizeWhenConfigIsInvalid() throws Exception {
        // Setup: invalid config value
        AppConfigDto config = AppConfigDto.builder()
            .name("cms.richtext.max_size_bytes")
            .configValue(objectMapper.readTree("\"invalid-json\""))
            .build();
        when(appConfigService.getConfig(AppConfigKey.CMS_RICHTEXT_MAX_SIZE_BYTES)).thenReturn(config);

        // Create small content (under default 1MB)
        JsonNode smallContent = objectMapper.readTree("""
            {
              "type": "doc",
              "content": [
                {
                  "type": "paragraph",
                  "content": [
                    {"type": "text", "text": "Small content"}
                  ]
                }
              ]
            }
            """);

        Map<String, JsonNode> bodyContent = Map.of("en", smallContent);

        // Should still validate successfully with default 1MB limit
        boolean result = validator.isValid(bodyContent, context);
        assertThat(result).isTrue();
    }
}
