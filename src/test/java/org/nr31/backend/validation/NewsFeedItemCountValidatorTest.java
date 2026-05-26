package org.nr31.backend.validation;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nr31.backend.dto.admin.AppConfigDto;
import org.nr31.backend.model.AppConfigKey;
import org.nr31.backend.service.AppConfigService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsFeedItemCountValidatorTest {

    @Mock
    private AppConfigService appConfigService;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    private NewsFeedItemCountValidator validator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private JsonNode toJsonNode(String val) {
        try {
            return objectMapper.readTree(val);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        validator = new NewsFeedItemCountValidator(appConfigService);
    }

    @Test
    void shouldReturnTrueForNullValue() {
        boolean result = validator.isValid(null, context);
        assertThat(result).isTrue();
        verifyNoInteractions(appConfigService);
    }

    @Test
    void shouldReturnTrueWhenItemCountIsBelowLimit() {
        AppConfigDto config = AppConfigDto.builder()
                .name("cms.newsfeed.max_items")
                .configValue(toJsonNode("50"))
                .build();
        when(appConfigService.getConfig(AppConfigKey.CMS_NEWSFEED_MAX_ITEMS)).thenReturn(config);

        // Test with itemCount well below limit
        boolean result = validator.isValid(10, context);
        assertThat(result).isTrue();
        verifyNoMoreInteractions(context);
    }

    @Test
    void shouldReturnTrueWhenItemCountIsAtLimit() {
        AppConfigDto config = AppConfigDto.builder()
                .name("cms.newsfeed.max_items")
                .configValue(toJsonNode("50"))
                .build();
        when(appConfigService.getConfig(AppConfigKey.CMS_NEWSFEED_MAX_ITEMS)).thenReturn(config);

        // Test with itemCount exactly at limit
        boolean result = validator.isValid(50, context);
        assertThat(result).isTrue();
        verifyNoMoreInteractions(context);
    }

    @Test
    void shouldReturnFalseWhenItemCountExceedsLimit() {
        AppConfigDto config = AppConfigDto.builder()
                .name("cms.newsfeed.max_items")
                .configValue(toJsonNode("50"))
                .build();
        when(appConfigService.getConfig(AppConfigKey.CMS_NEWSFEED_MAX_ITEMS)).thenReturn(config);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

        // Test with itemCount exceeding limit
        boolean result = validator.isValid(51, context);
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(
                "cms_validation.newsfeed.item_count_exceeded|max=50|actual=51");
    }

    @Test
    void shouldThrowExceptionWhenConfigIsInvalidJson() {
        // Setup: invalid config value (not a valid JSON fragment)
        AppConfigDto config = AppConfigDto.builder()
                .name("cms.newsfeed.max_items")
                .configValue(toJsonNode("\"invalid-json\""))
                .build();
        when(appConfigService.getConfig(AppConfigKey.CMS_NEWSFEED_MAX_ITEMS)).thenReturn(config);

        // Test that it throws RuntimeException
        assertThrows(RuntimeException.class, () ->
                validator.isValid(5, context),
                "Unable to parse cms.newsfeed.max_items config");
    }

    @Test
    void shouldThrowExceptionWhenConfigValueIsNonNumericString() {
        // Setup: config value is valid JSON string but not a number
        AppConfigDto config = AppConfigDto.builder()
                .name("cms.newsfeed.max_items")
                .configValue(toJsonNode("\"not-a-number\""))
                .build();
        when(appConfigService.getConfig(AppConfigKey.CMS_NEWSFEED_MAX_ITEMS)).thenReturn(config);

        // Test that it throws RuntimeException
        assertThrows(RuntimeException.class, () ->
                validator.isValid(8, context),
                "Unable to parse cms.newsfeed.max_items config");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenConfigIsMissing() {
        // Setup: config service returns null
        when(appConfigService.getConfig(AppConfigKey.CMS_NEWSFEED_MAX_ITEMS)).thenReturn(null);

        // Test that it throws NullPointerException when accessing config.getConfigValue()
        assertThrows(RuntimeException.class, () -> {
            validator.isValid(5, context);
        });
    }

    @Test
    void shouldHandleMinimumValidItemCount() {
        AppConfigDto config = AppConfigDto.builder()
                .name("cms.newsfeed.max_items")
                .configValue(toJsonNode("50"))
                .build();
        when(appConfigService.getConfig(AppConfigKey.CMS_NEWSFEED_MAX_ITEMS)).thenReturn(config);

        // Test with itemCount of 1 (minimum valid value per @Min(1) annotation)
        boolean result = validator.isValid(1, context);
        assertThat(result).isTrue();
        verifyNoMoreInteractions(context);
    }

    @Test
    void shouldProvideCorrectErrorMessageWithActualValues() {
        AppConfigDto config = AppConfigDto.builder()
                .name("cms.newsfeed.max_items")
                .configValue(toJsonNode("25"))
                .build();
        when(appConfigService.getConfig(AppConfigKey.CMS_NEWSFEED_MAX_ITEMS)).thenReturn(config);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

        // Test with itemCount of 30
        boolean result = validator.isValid(30, context);
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(
                "cms_validation.newsfeed.item_count_exceeded|max=25|actual=30");
    }
}
