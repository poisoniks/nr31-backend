package org.nr31.backend.exception;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.validation.FieldError;
import org.nr31.backend.dto.ValidationErrorResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(code = HttpStatus.FORBIDDEN)
    public ErrorResponse handleDisabledException(DisabledException e) {
        log.debug("Account disabled: {}", e.getMessage());
        return new ErrorResponse("Account is disabled", LocalDateTime.now());
    }

    @ExceptionHandler(LockedException.class)
    @ResponseStatus(code = HttpStatus.FORBIDDEN)
    public ErrorResponse handleLockedException(LockedException e) {
        log.debug("Account locked: {}", e.getMessage());
        return new ErrorResponse("Account is locked", LocalDateTime.now());
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(code = HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleBadCredentialsException(BadCredentialsException e) {
        log.debug("Bad credentials: {}", e.getMessage());
        return new ErrorResponse("Invalid username or password", LocalDateTime.now());
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(code = HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationException(AuthenticationException e) {
        log.debug("Authentication exception: {}", e.getMessage());
        return new ErrorResponse("Unable to authenticate", LocalDateTime.now());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    @ResponseStatus(code = HttpStatus.FORBIDDEN)
    public ErrorResponse handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        log.debug("Access denied: {}", e.getMessage());
        return new ErrorResponse("Access denied", LocalDateTime.now());
    }

    @ExceptionHandler(JwtException.class)
    @ResponseStatus(code = HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleJwtException(JwtException e) {
        log.debug("Invalid or expired token", e);
        return new ErrorResponse("Authorization token is invalid or expired", LocalDateTime.now());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNoResourceFoundException(NoResourceFoundException e) {
        String message = "Resource " + e.getResourcePath() + " is not found";
        log.debug(message, e);
        return new ErrorResponse(message, LocalDateTime.now());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        String message = "Invalid request body";
        log.debug(message, e);
        return new ErrorResponse(message, LocalDateTime.now());
    }

    @ExceptionHandler(TokenRefreshException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTokenRefreshException(TokenRefreshException e) {
        log.debug("Refresh token is invalid or expired", e);
        return new ErrorResponse("Invalid refresh token", LocalDateTime.now());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = (error instanceof FieldError) ? ((FieldError) error).getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.debug("Validation failed: {}", errors);

        return ValidationErrorResponse.builder()
                .message("Request validation failed")
                .details(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        Map<String, String> errors = new HashMap<>();
        errors.put(e.getName(), "Invalid format");

        log.debug("Type mismatch failed: {}", errors);

        return ValidationErrorResponse.builder()
                .message("Request validation failed")
                .details(errors)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.debug("Http method not supported exception", e);
        return new ErrorResponse(String.format("Request method '%s' is not supported", e.getMethod()), LocalDateTime.now());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.debug("Request param missing", e);
        return new ErrorResponse(String.format("Request param '%s' is required", e.getParameterName()), LocalDateTime.now());
    }

    @ExceptionHandler(CalendarException.UserError.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUserCalendarException(CalendarException e) {
        log.debug("User calendar exception", e);
        return new ErrorResponse(e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException e) {
        log.debug("Illegal argument exception", e);
        return new ErrorResponse(e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(ElementNotFoundException.class)
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    public ErrorResponse handleElementNotFoundException(ElementNotFoundException e) {
        log.debug("Element not found exception", e);
        return new ErrorResponse("Requested entity is not found", LocalDateTime.now());
    }

    @ExceptionHandler(AppConfigException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleAppConfigException(AppConfigException e) {
        log.debug("App config exception", e);
        return new ErrorResponse(e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(AppConfigValidationException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleAppConfigValidationException(AppConfigValidationException e) {
        log.debug("AppConfig validation exception", e);
        return ValidationErrorResponse.builder()
                .message("App config validation failed")
                .details(e.getErrors())
                .timestamp(LocalDateTime.now())
                .build();
    }

    @ExceptionHandler(CalendarException.ServerError.class)
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleServerCalendarException(CalendarException e) {
        log.error("Server calendar exception", e);
        return new ErrorResponse("Unable to process the calendar request", LocalDateTime.now());
    }

    @ExceptionHandler(FeatureDisabledException.class)
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    public ErrorResponse handleFeatureDisabledException(FeatureDisabledException e) {
        log.debug("Feature disabled: {}", e.getMessage());
        return new ErrorResponse("Requested endpoint is disabled", LocalDateTime.now());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(code = HttpStatus.CONFLICT)
    public ErrorResponse handleConflictException(ConflictException e) {
        log.debug("Conflict exception: {}", e.getMessage());
        return new ErrorResponse(e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(FileStorageException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public ErrorResponse handleFileStorageException(FileStorageException e) {
        log.debug("File storage exception: {}", e.getMessage());
        return new ErrorResponse(e.getMessage(), LocalDateTime.now());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(code = HttpStatus.CONTENT_TOO_LARGE)
    public ErrorResponse handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.debug("File upload size exceeded: {}", e.getMessage());
        return new ErrorResponse("File size exceeds the maximum allowed limit of 5MB", LocalDateTime.now());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGlobalException(Exception e) {
        log.error("Unknown exception", e);
        return new ErrorResponse("Internal server error", LocalDateTime.now());
    }
}
