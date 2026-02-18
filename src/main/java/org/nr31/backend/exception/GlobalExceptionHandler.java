package org.nr31.backend.exception;

import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    @ExceptionHandler(Exception.class)
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGlobalException(Exception e) {
        log.error("Unknown exception", e);
        return new ErrorResponse("Internal server error", LocalDateTime.now());
    }
}
