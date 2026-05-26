package org.nr31.backend.exception;

import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.dto.common.ErrorCode;
import org.nr31.backend.dto.common.ErrorResponse;
import org.nr31.backend.dto.common.ValidationErrorResponse;
import org.nr31.backend.dto.cms.CmsValidationErrorResponse;
import org.nr31.backend.dto.cms.UpdateDraftRequest;
import org.nr31.backend.dto.cms.WidgetDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.InvalidTypeIdException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleDisabledException(DisabledException e) {
        log.debug("Account disabled: {}", e.getMessage());
        return ErrorResponse.builder()
                .message("Account is disabled")
                .code(ErrorCode.ACCOUNT_DISABLED)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(LockedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleLockedException(LockedException e) {
        log.debug("Account locked: {}", e.getMessage());
        return ErrorResponse.builder()
                .message("Account is locked")
                .code(ErrorCode.ACCOUNT_LOCKED)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleBadCredentialsException(BadCredentialsException e) {
        log.debug("Bad credentials: {}", e.getMessage());
        return ErrorResponse.builder()
                .message("Invalid username or password")
                .code(ErrorCode.BAD_CREDENTIALS)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationException(AuthenticationException e) {
        log.debug("Authentication exception: {}", e.getMessage());
        return ErrorResponse.builder()
                .message("Unable to authenticate")
                .code(ErrorCode.UNAUTHORIZED)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        log.debug("Access denied: {}", e.getMessage());
        return ErrorResponse.builder()
                .message("Access denied")
                .code(ErrorCode.FORBIDDEN)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDeniedException(AccessDeniedException e) {
        log.debug("Access denied: {}", e.getMessage());
        return ErrorResponse.builder()
                .message("Access denied")
                .code(ErrorCode.FORBIDDEN)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(JwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleJwtException(JwtException e) {
        log.debug("Invalid or expired token", e);
        return ErrorResponse.builder()
                .message("Authorization token is invalid or expired")
                .code(ErrorCode.INVALID_TOKEN)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNoResourceFoundException(NoResourceFoundException e) {
        String message = "Resource " + e.getResourcePath() + " is not found";
        log.debug(message, e);
        return ErrorResponse.builder()
                .message(message)
                .code(ErrorCode.ELEMENT_NOT_FOUND)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof InvalidTypeIdException) {
            return handleInvalidTypeIdException((InvalidTypeIdException) e.getCause());
        }
        String message = "Invalid request body";
        log.debug(message, e);
        return ErrorResponse.builder()
                .message(message)
                .code(ErrorCode.VALIDATION_ERROR)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(TokenRefreshException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTokenRefreshException(TokenRefreshException e) {
        log.debug("Refresh token is invalid or expired", e);
        return ErrorResponse.builder()
                .message("Invalid refresh token")
                .code(ErrorCode.TOKEN_EXPIRED)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ApiResponse(responseCode = "400", description = "Validation error",
            content = @Content(schema = @Schema(anyOf = {ValidationErrorResponse.class, CmsValidationErrorResponse.class})))
    public ErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        if (e.getBindingResult().getTarget() instanceof UpdateDraftRequest) {
            CmsValidationErrorResponse response = CmsValidationHandler.buildCmsValidationResponse(e);
            log.debug("CMS Validation failed: details={}, context={}", response.getDetails(), response.getContext());
            return response;
        }

        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = (error instanceof FieldError) ? ((FieldError) error).getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.debug("Validation failed: {}", errors);

        return ValidationErrorResponse.builder()
                .message("Request validation failed")
                .code(ErrorCode.VALIDATION_ERROR)
                .details(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        Map<String, String> errors = new HashMap<>();
        errors.put(e.getName(), "Invalid format");

        log.debug("Type mismatch failed: {}", errors);

        return ValidationErrorResponse.builder()
                .message("Request validation failed")
                .code(ErrorCode.VALIDATION_ERROR)
                .details(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.debug("Http method not supported exception", e);
        return ErrorResponse.builder()
                .message(String.format("Request method '%s' is not supported", e.getMethod()))
                .code(ErrorCode.ENDPOINT_NOT_FOUND)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.debug("Request param missing", e);
        return ErrorResponse.builder()
                .message(String.format("Request param '%s' is required", e.getParameterName()))
                .code(ErrorCode.VALIDATION_ERROR)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(CalendarException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleCalendarException(CalendarException e) {
        log.debug("Calendar exception: {}", e.getMessage());
        return ErrorResponse.builder()
                .message(e.getMessage())
                .code(e.getErrorCode())
                .metadata(e.getMetadata())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException e) {
        log.debug("Illegal argument exception", e);
        return ErrorResponse.builder()
                .message(e.getMessage())
                .code(ErrorCode.VALIDATION_ERROR)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(ElementNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleElementNotFoundException(ElementNotFoundException e) {
        log.debug("Element not found exception", e);
        return ErrorResponse.builder()
                .message(e.getMessage())
                .code(e.getErrorCode())
                .metadata(e.getMetadata())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(AppConfigException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleAppConfigException(AppConfigException e) {
        log.debug("App config exception", e);
        return ErrorResponse.builder()
                .message(e.getMessage())
                .code(ErrorCode.INTERNAL_SERVER_ERROR)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(AppConfigValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleAppConfigValidationException(AppConfigValidationException e) {
        log.debug("AppConfig validation exception", e);
        return ValidationErrorResponse.builder()
                .message("App config validation failed")
                .code(ErrorCode.VALIDATION_ERROR)
                .details(e.getErrors())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(FeatureDisabledException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleFeatureDisabledException(FeatureDisabledException e) {
        log.debug("Feature disabled: {}", e.getMessage());
        return ErrorResponse.builder()
                .message("Requested endpoint is disabled")
                .code(ErrorCode.FEATURE_DISABLED)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflictException(ConflictException e) {
        log.debug("Conflict exception: {}", e.getMessage());
        return ErrorResponse.builder()
                .message(e.getMessage())
                .code(e.getErrorCode())
                .metadata(e.getMetadata())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(RateLimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ErrorResponse handleRateLimitException(RateLimitException e) {
        log.debug("Too many requests: {}", e.getMessage());
        return ErrorResponse.builder()
                .message(e.getMessage())
                .code(e.getErrorCode())
                .metadata(e.getMetadata())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(KeyExpiredException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleKeyExpiredException(KeyExpiredException e) {
        log.debug(e.getMessage(), e);
        return ErrorResponse.builder()
                .message(e.getMessage())
                .code(e.getErrorCode())
                .metadata(e.getMetadata())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(FileStorageException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleFileStorageException(FileStorageException e) {
        log.debug("File storage exception: {}", e.getMessage());
        return ErrorResponse.builder()
                .message(e.getMessage())
                .code(e.getErrorCode())
                .metadata(e.getMetadata())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.CONTENT_TOO_LARGE)
    public ErrorResponse handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.debug("File upload size exceeded: {}", e.getMessage());
        return ErrorResponse.builder()
                .message("File size exceeds the maximum allowed limit of 5MB")
                .code(ErrorCode.FILE_TOO_LARGE)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(InvalidTypeIdException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidTypeIdException(InvalidTypeIdException e) {
        String message;
        ErrorCode code;
        Map<String, Object> metadata = new HashMap<>();

        if (e.getBaseType().getRawClass().equals(WidgetDto.class)) {
            message = String.format("Widget type '%s' does not exist", e.getTypeId());
            code = ErrorCode.INVALID_WIDGET_TYPE;
            metadata.put("type", e.getTypeId());
        } else {
            message = "Invalid cms component";
            code = ErrorCode.VALIDATION_ERROR;
        }
        log.debug(message, e);

        return ErrorResponse.builder()
                .message(message)
                .code(code)
                .timestamp(LocalDateTime.now())
                .metadata(metadata)
                .build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGlobalException(Exception e) {
        log.error("Unknown exception", e);
        return ErrorResponse.builder()
                .message("Internal server error")
                .code(ErrorCode.INTERNAL_SERVER_ERROR)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
