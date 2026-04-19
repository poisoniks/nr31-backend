package org.nr31.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Generic error response")
public class ErrorResponse {
    @Schema(description = "Error message", example = "An unexpected error occurred", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "Standardized error code", example = "INTERNAL_SERVER_ERROR", requiredMode = Schema.RequiredMode.REQUIRED)
    private ErrorCode code;

    @Schema(description = "Timestamp when the error occurred", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime timestamp;

    @Schema(description = """
            Standardized error codes for the application.
            
            ### Authentication & Authorization
            * `UNAUTHORIZED`: User is not authenticated
            * `FORBIDDEN`: User does not have required permissions
            * `INVALID_TOKEN`: Provided token is invalid
            * `TOKEN_EXPIRED`: Provided token has expired
            * `BAD_CREDENTIALS`: Invalid username or password
            * `ACCOUNT_LOCKED`: Account is locked due to too many failed attempts
            * `ACCOUNT_DISABLED`: Account is disabled by administrator
            
            ### Resource Errors
            * `ELEMENT_NOT_FOUND`: Requested element not found. Metadata: 'id' (UUID or Long) or other identifier
            * `FILE_NOT_FOUND`: Requested file not found. Metadata: 'id' (UUID)
            * `FOLDER_NOT_FOUND`: Requested folder not found. Metadata: 'id' (UUID)
            * `PARENT_FOLDER_NOT_FOUND`: Parent folder for the operation not found. Metadata: 'id' (UUID)
            * `ROLE_NOT_FOUND`: Role not found. Metadata: 'id' (Long)
            * `PERMISSION_NOT_FOUND`: Permission not found. Metadata: 'id' (Long)
            * `USER_NOT_FOUND`: User not found. Metadata: 'id' (Long) or 'username' (String)
            * `EVENT_NOT_FOUND`: Calendar event not found. Metadata: 'id' (String/UUID)
            * `UNIT_TYPE_NOT_FOUND`: Unit type not found. Metadata: 'id' (Long)
            * `EVENT_TYPE_NOT_FOUND`: Event type not found. Metadata: 'id' (Long)
            * `CONFIG_NOT_FOUND`: System configuration not found. Metadata: 'name' (String)
            * `ENDPOINT_NOT_FOUND`: Requested endpoint not found
            
            ### File Handling
            * `INVALID_FILE_TYPE`: File type is not allowed for this operation
            * `FILE_TOO_LARGE`: File size exceeds the maximum limit
            * `EMPTY_FILE`: Cannot process an empty file
            * `QUOTA_EXCEEDED`: User has exceeded their storage quota. Metadata: 'currentSize' (Long), 'maxQuota' (Long), 'fileSize' (Long)
            * `STORAGE_ERROR`: Generic storage error. Metadata: 'path' (String) or 'id' (UUID)
            
            ### Logic & Validation
            * `VALIDATION_ERROR`: Request validation failed. Detailed errors in 'details' field for ValidationErrorResponse
            * `CONFLICT`: Operation conflicts with the current state of the resource
            * `FOLDER_NOT_EMPTY`: Cannot delete a folder that is not empty. Metadata: 'id' (UUID)
            * `FEATURE_DISABLED`: The requested feature is currently disabled
            
            ### Server Errors
            * `INTERNAL_SERVER_ERROR`: An unexpected error occurred on the server""")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> metadata;
}
