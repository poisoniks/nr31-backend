package org.nr31.backend.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nr31.backend.dto.AppConfigDto;
import org.nr31.backend.service.AppConfigService;

import static org.assertj.core.api.Assertions.assertThat;
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

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        validator = new NewsFeedItemCountValidator(appConfigService, objectMapper);
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
                .configValue("50")
                .build();
        when(appConfigService.getConfig("cms.newsfeed.max_items")).thenReturn(config);

        // Test with itemCount well below limit
        boolean result = validator.isValid(10, context);
        assertThat(result).isTrue();
        verifyNoMoreInteractions(context);
    }

    @Test
    void shouldReturnTrueWhenItemCountIsAtLimit() {
        AppConfigDto config = AppConfigDto.builder()
                .name("cms.newsfeed.max_items")
                .configValue("50")
                .build();
        when(appConfigService.getConfig("cms.newsfeed.max_items")).thenReturn(config);

        // Test with itemCount exactly at limit
        boolean result = validator.isValid(50, context);
        assertThat(result).isTrue();
        verifyNoMoreInteractions(context);
    }

    @Test
    void shouldReturnFalseWhenItemCountExceedsLimit() {
        AppConfigDto config = AppConfigDto.builder()
                .name("cms.newsfeed.max_items")
                .configValue("50")
                .build();
        when(appConfigService.getConfig("cms.newsfeed.max_items")).thenReturn(config);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

        // Test with itemCount exceeding limit
        boolean result = validator.isValid(51, context);
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(
                "Item count exceeds maximum allowed of 50 (actual: 51)");
    }

    @Test
    void shouldThrowExceptionWhenConfigIsInvalidJson() {
        // Setup: invalid config value (not a valid JSON fragment)
        AppConfigDto config = AppConfigDto.builder()
                .name("cms.newsfeed.max_items")
                .configValue("invalid-json")
                .build();
        when(appConfigService.getConfig("cms.newsfeed.max_items")).thenReturn(config);

        // Test that it throws RuntimeException
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
                validator.isValid(5, context),
                "Unable to parse cms.newsfeed.max_items config");
    }

    @Test
    void shouldReturnFalseWhenConfigValueIsNonNumericString() {
        // Setup: config value is valid JSON string but not a number (Jackson's asInt() returns 0 for non-numeric strings)
        AppConfigDto config = AppConfigDto.builder()
                .name("cms.newsfeed.max_items")
                .configValue("\"not-a-number\"")
                .build();
        when(appConfigService.getConfig("cms.newsfeed.max_items")).thenReturn(config);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

        // Test with itemCount - will fail because asInt() on non-numeric string returns 0
        boolean result = validator.isValid(8, context);
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(
                "Item count exceeds maximum allowed of 0 (actual: 8)");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenConfigIsMissing() {
        // Setup: config service returns null
        when(appConfigService.getConfig("cms.newsfeed.max_items")).thenReturn(null);

        // Test that it throws NullPointerException when accessing config.getConfigValue()
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> {
            validator.isValid(5, context);
        });
    }

    @Test
    void shouldHandleMinimumValidItemCount() {
        AppConfigDto config = AppConfigDto.builder()
                .name("cms.newsfeed.max_items")
                .configValue("50")
                .build();
        when(appConfigService.getConfig("cms.newsfeed.max_items")).thenReturn(config);

        // Test with itemCount of 1 (minimum valid value per @Min(1) annotation)
        boolean result = validator.isValid(1, context);
        assertThat(result).isTrue();
        verifyNoMoreInteractions(context);
    }

    @Test
    void shouldProvideCorrectErrorMessageWithActualValues() {
        AppConfigDto config = AppConfigDto.builder()
                .name("cms.newsfeed.max_items")
                .configValue("25")
                .build();
        when(appConfigService.getConfig("cms.newsfeed.max_items")).thenReturn(config);

        when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(violationBuilder);

        // Test with itemCount of 30
        boolean result = validator.isValid(30, context);
        assertThat(result).isFalse();
        verify(context).disableDefaultConstraintViolation();
        verify(context).buildConstraintViolationWithTemplate(
                "Item count exceeds maximum allowed of 25 (actual: 30)");
    }
}
