package org.nr31.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request object for logging out and invalidating refresh tokens")
public class LogoutRequest {
    @Schema(description = "The refresh token to invalidate", example = "550e8400-e29b-41d4-a716-446655440000")
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
