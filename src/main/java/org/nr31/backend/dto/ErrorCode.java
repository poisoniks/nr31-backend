package org.nr31.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standardized error codes for the application")
public enum ErrorCode {
    // Authentication & Authorization
    @Schema(description = "User is not authenticated")
    UNAUTHORIZED,
    @Schema(description = "User does not have required permissions")
    FORBIDDEN,
    @Schema(description = "Provided token is invalid")
    INVALID_TOKEN,
    @Schema(description = "Provided token has expired")
    TOKEN_EXPIRED,
    @Schema(description = "Invalid username or password")
    BAD_CREDENTIALS,
    @Schema(description = "Account is locked due to too many failed attempts")
    ACCOUNT_LOCKED,
    @Schema(description = "Account is disabled by administrator")
    ACCOUNT_DISABLED,

    // Resource errors
    @Schema(description = "Requested element not found. Metadata: 'id' (UUID or Long) or other identifier")
    ELEMENT_NOT_FOUND,
    @Schema(description = "Requested file not found. Metadata: 'id' (UUID)")
    FILE_NOT_FOUND,
    @Schema(description = "Requested folder not found. Metadata: 'id' (UUID)")
    FOLDER_NOT_FOUND,
    @Schema(description = "Parent folder for the operation not found. Metadata: 'id' (UUID)")
    PARENT_FOLDER_NOT_FOUND,
    @Schema(description = "Role not found. Metadata: 'id' (Long)")
    ROLE_NOT_FOUND,
    @Schema(description = "Permission not found. Metadata: 'id' (Long)")
    PERMISSION_NOT_FOUND,
    @Schema(description = "User not found. Metadata: 'id' (Long) or 'username' (String)")
    USER_NOT_FOUND,
    @Schema(description = "Calendar event not found. Metadata: 'id' (String/UUID)")
    EVENT_NOT_FOUND,
    @Schema(description = "Unit type not found. Metadata: 'id' (Long)")
    UNIT_TYPE_NOT_FOUND,
    @Schema(description = "Event type not found. Metadata: 'id' (Long)")
    EVENT_TYPE_NOT_FOUND,
    @Schema(description = "System configuration not found. Metadata: 'name' (String)")
    CONFIG_NOT_FOUND,
    @Schema(description = "Requested endpoint not found")
    ENDPOINT_NOT_FOUND,

    // File handling
    @Schema(description = "File type is not allowed for this operation")
    INVALID_FILE_TYPE,
    @Schema(description = "File size exceeds the maximum limit")
    FILE_TOO_LARGE,
    @Schema(description = "Cannot process an empty file")
    EMPTY_FILE,
    @Schema(description = "User has exceeded their storage quota. Metadata: 'currentSize' (Long), 'maxQuota' (Long), 'fileSize' (Long)")
    QUOTA_EXCEEDED,
    @Schema(description = "Generic storage error. Metadata: 'path' (String) or 'id' (UUID)")
    STORAGE_ERROR,

    // Logic & Validation
    @Schema(description = "Request validation failed. Detailed errors in 'details' field for ValidationErrorResponse")
    VALIDATION_ERROR,
    @Schema(description = "Operation conflicts with the current state of the resource")
    CONFLICT,
    @Schema(description = "Cannot delete a folder that is not empty. Metadata: 'id' (UUID)")
    FOLDER_NOT_EMPTY,
    @Schema(description = "The requested feature is currently disabled")
    FEATURE_DISABLED,
    
    // Server errors
    @Schema(description = "An unexpected error occurred on the server")
    INTERNAL_SERVER_ERROR
}
