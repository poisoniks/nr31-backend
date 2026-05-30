package org.nr31.backend.validation;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintValidatorContext;
import net.jqwik.api.*;
import org.nr31.backend.dto.admin.AppConfigDto;
import org.nr31.backend.model.AppConfigKey;
import org.nr31.backend.dto.cms.RichTextWidgetDto;
import org.nr31.backend.service.AppConfigService;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * These tests use jqwik to generate many random RichTextWidgetDto instances
 * with different content sizes to verify the validator behaves correctly
 * across a wide range of inputs.
 */
class RichTextSizeValidatorPropertyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Property(tries = 100)
    @Label("RichTextSizeValidator accepts content within size limit")
    void validatorAcceptsContentWithinLimit(
        @ForAll("richTextContentWithinLimit") RichTextContentTestCase testCase
    ) {
        AppConfigService appConfigService = mock(AppConfigService.class);
        AppConfigDto config = AppConfigDto.builder()
            .name("cms.richtext.max_size_bytes")
            .configValue(objectMapper.readTree(String.valueOf(testCase.maxSizeBytes)))
            .build();
        when(appConfigService.getConfig(AppConfigKey.CMS_RICHTEXT_MAX_SIZE_BYTES)).thenReturn(config);

        RichTextSizeValidator validator = new RichTextSizeValidator(appConfigService, objectMapper);
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

        RichTextWidgetDto widget = new RichTextWidgetDto();
        widget.setBodyContent(testCase.bodyContent);

        boolean result = validator.isValid(testCase.bodyContent, context);

        assertThat(result)
            .as("Validator should accept content within size limit (max: %d bytes, actual: %d bytes)",
                testCase.maxSizeBytes, testCase.actualSizeBytes)
            .isTrue();
    }

    @Property(tries = 100)
    @Label("RichTextSizeValidator rejects content exceeding size limit")
    void validatorRejectsContentExceedingLimit(
        @ForAll("richTextContentExceedingLimit") RichTextContentTestCase testCase
    ) {
        AppConfigService appConfigService = mock(AppConfigService.class);
        AppConfigDto config = AppConfigDto.builder()
            .name("cms.richtext.max_size_bytes")
            .configValue(objectMapper.readTree(String.valueOf(testCase.maxSizeBytes)))
            .build();
        when(appConfigService.getConfig(AppConfigKey.CMS_RICHTEXT_MAX_SIZE_BYTES)).thenReturn(config);

        RichTextSizeValidator validator = new RichTextSizeValidator(appConfigService, objectMapper);
        ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder = 
            mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        
        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

        RichTextWidgetDto widget = new RichTextWidgetDto();
        widget.setBodyContent(testCase.bodyContent);

        boolean result = validator.isValid(testCase.bodyContent, context);

        assertThat(result)
            .as("Validator should reject content exceeding size limit (max: %d bytes, actual: %d bytes)",
                testCase.maxSizeBytes, testCase.actualSizeBytes)
            .isFalse();

        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(contains("cms_validation.richtext.size_exceeded"));
    }

    @Provide
    Arbitrary<RichTextContentTestCase> richTextContentWithinLimit() {
        return Arbitraries.integers().between(500, 10000).flatMap(maxSize -> {
            // Generate content that is definitely within the limit (50-90% of max size)
            int targetSize = (int) (maxSize * 0.5 + Math.random() * maxSize * 0.4);
            
            return Arbitraries.of("en", "uk", "de", "fr", "es").flatMap(locale -> {
                try {
                    // Generate TipTap JSON content of approximately target size
                    String textContent = generateTextOfSize(targetSize / 2);
                    JsonNode content = objectMapper.readTree(String.format("""
                        {
                          "type": "doc",
                          "content": [
                            {
                              "type": "paragraph",
                              "content": [
                                {"type": "text", "text": "%s"}
                              ]
                            }
                          ]
                        }
                        """, escapeJson(textContent)));
                    
                    Map<String, JsonNode> bodyContent = Map.of(locale, content);
                    
                    // Calculate actual size
                    String serialized = objectMapper.writeValueAsString(content);
                    int actualSize = serialized.getBytes(StandardCharsets.UTF_8).length;
                    
                    // Only return if actually within limit
                    if (actualSize <= maxSize) {
                        return Arbitraries.just(new RichTextContentTestCase(maxSize, actualSize, bodyContent));
                    } else {
                        // Fallback to minimal content
                        JsonNode minimalContent = objectMapper.readTree("""
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
                        Map<String, JsonNode> minimalBodyContent = Map.of(locale, minimalContent);
                        String minimalSerialized = objectMapper.writeValueAsString(minimalContent);
                        int minimalSize = minimalSerialized.getBytes(StandardCharsets.UTF_8).length;
                        return Arbitraries.just(new RichTextContentTestCase(maxSize, minimalSize, minimalBodyContent));
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to generate test case", e);
                }
            });
        });
    }

    @Provide
    Arbitrary<RichTextContentTestCase> richTextContentExceedingLimit() {
        return Arbitraries.integers().between(100, 5000).flatMap(maxSize -> {
            // Generate content that exceeds the limit (110-200% of max size)
            int targetSize = (int) (maxSize * 1.1 + Math.random() * maxSize * 0.9);
            
            return Arbitraries.of("en", "uk", "de", "fr", "es").flatMap(locale -> {
                try {
                    // Generate TipTap JSON content of approximately target size
                    String textContent = generateTextOfSize(targetSize / 2);
                    JsonNode content = objectMapper.readTree(String.format("""
                        {
                          "type": "doc",
                          "content": [
                            {
                              "type": "paragraph",
                              "content": [
                                {"type": "text", "text": "%s"}
                              ]
                            }
                          ]
                        }
                        """, escapeJson(textContent)));
                    
                    Map<String, JsonNode> bodyContent = Map.of(locale, content);
                    
                    // Calculate actual size
                    String serialized = objectMapper.writeValueAsString(content);
                    int actualSize = serialized.getBytes(StandardCharsets.UTF_8).length;
                    
                    // Only return if actually exceeds limit
                    if (actualSize > maxSize) {
                        return Arbitraries.just(new RichTextContentTestCase(maxSize, actualSize, bodyContent));
                    } else {
                        // Force it to exceed by adding more content
                        String largeText = generateTextOfSize(maxSize * 2);
                        JsonNode largeContent = objectMapper.readTree(String.format("""
                            {
                              "type": "doc",
                              "content": [
                                {
                                  "type": "paragraph",
                                  "content": [
                                    {"type": "text", "text": "%s"}
                                  ]
                                }
                              ]
                            }
                            """, escapeJson(largeText)));
                        Map<String, JsonNode> largeBodyContent = Map.of(locale, largeContent);
                        String largeSerialized = objectMapper.writeValueAsString(largeContent);
                        int largeSize = largeSerialized.getBytes(StandardCharsets.UTF_8).length;
                        return Arbitraries.just(new RichTextContentTestCase(maxSize, largeSize, largeBodyContent));
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to generate test case", e);
                }
            });
        });
    }

    private String generateTextOfSize(int targetSize) {
        StringBuilder sb = new StringBuilder(targetSize);
        String sample = "Lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore et dolore magna aliqua ";
        
        while (sb.length() < targetSize) {
            sb.append(sample);
        }
        
        return sb.substring(0, targetSize);
    }

    private String escapeJson(String text) {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private record RichTextContentTestCase(
        int maxSizeBytes,
        int actualSizeBytes,
        Map<String, JsonNode> bodyContent
    ) {}
}
