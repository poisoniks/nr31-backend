package org.nr31.backend.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.nr31.backend.dto.ErrorCode;
import org.nr31.backend.dto.ErrorResponse;
import org.nr31.backend.dto.ValidationErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerCmsTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void elementNotFoundException_Returns404WithMetadata() {
        // Given: ElementNotFoundException with slug metadata
        String slug = "non-existent-page";
        Map<String, Object> metadata = Map.of("slug", slug);
        ElementNotFoundException exception = new ElementNotFoundException(
            "Page not found",
            ErrorCode.ELEMENT_NOT_FOUND,
            metadata
        );

        // When: Handler processes the exception
        ErrorResponse response = exceptionHandler.handleElementNotFoundException(exception);

        // Then: Should return 404 with metadata
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Page not found");
        assertThat(response.getCode()).isEqualTo(ErrorCode.ELEMENT_NOT_FOUND);
        assertThat(response.getMetadata())
            .isNotNull()
            .containsEntry("slug", slug);
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void conflictException_Returns409WithVersionMetadata() {
        // Given: ConflictException with version conflict metadata
        String slug = "home";
        Integer providedVersion = 5;
        Integer currentVersion = 7;
        Map<String, Object> metadata = Map.of(
            "slug", slug,
            "providedVersion", providedVersion,
            "currentVersion", currentVersion
        );
        ConflictException exception = new ConflictException(
            "Version conflict: page was modified by another user",
            ErrorCode.CONFLICT,
            metadata
        );

        // When: Handler processes the exception
        ErrorResponse response = exceptionHandler.handleConflictException(exception);

        // Then: Should return 409 with version metadata
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).contains("Version conflict");
        assertThat(response.getCode()).isEqualTo(ErrorCode.CONFLICT);
        assertThat(response.getMetadata())
            .isNotNull()
            .containsEntry("slug", slug)
            .containsEntry("providedVersion", providedVersion)
            .containsEntry("currentVersion", currentVersion);
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void appConfigValidationException_Returns400WithFieldDetails() {
        // Given: AppConfigValidationException with field errors
        Map<String, String> errors = Map.of(
            "slots[0].widgets[1]", "Widget type 'video' is not allowed in slot type 'sidebar'"
        );
        AppConfigValidationException exception = new AppConfigValidationException(
            "Layout validation failed",
            errors
        );

        // When: Handler processes the exception
        ValidationErrorResponse response = exceptionHandler.handleAppConfigValidationException(exception);

        // Then: Should return 400 with field details
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("App config validation failed");
        assertThat(response.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(response.getDetails())
            .isNotNull()
            .containsEntry("slots[0].widgets[1]", "Widget type 'video' is not allowed in slot type 'sidebar'");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void httpMessageNotReadableException_Returns400ForInvalidJson() {
        // Given: HttpMessageNotReadableException for malformed JSON
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
        when(exception.getMessage()).thenReturn("JSON parse error");

        // When: Handler processes the exception
        ErrorResponse response = exceptionHandler.handleHttpMessageNotReadableException(exception);

        // Then: Should return 400 with validation error code
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Invalid request body");
        assertThat(response.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void illegalArgumentException_Returns400() {
        // Given: IllegalArgumentException from validation logic
        IllegalArgumentException exception = new IllegalArgumentException(
            "Widget type 'unknown' is not allowed in slot type 'hero'"
        );

        // When: Handler processes the exception
        ErrorResponse response = exceptionHandler.handleIllegalArgumentException(exception);

        // Then: Should return 400 with validation error code
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).contains("Widget type");
        assertThat(response.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(response.getTimestamp()).isNotNull();
    }

    /**
     * Verify that MethodArgumentNotValidException returns 400 with field details
     * This is used for @Valid annotation validation on request DTOs
     */
    @Test
    void methodArgumentNotValidException_Returns400WithFieldDetails() {
        // Given: MethodArgumentNotValidException with field errors
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        FieldError fieldError = new FieldError("updateDraftRequest", "version", "Version must not be null");
        
        when(exception.getBindingResult()).thenReturn(
            new org.springframework.validation.BeanPropertyBindingResult(new Object(), "updateDraftRequest")
        );
        exception.getBindingResult().addError(fieldError);

        // When: Handler processes the exception
        ValidationErrorResponse response = exceptionHandler.handleMethodArgumentNotValidException(exception);

        // Then: Should return 400 with field details
        assertThat(response).isNotNull();
        assertThat(response.getMessage()).isEqualTo("Request validation failed");
        assertThat(response.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(response.getDetails()).isNotNull();
        assertThat(response.getTimestamp()).isNotNull();
    }
}
