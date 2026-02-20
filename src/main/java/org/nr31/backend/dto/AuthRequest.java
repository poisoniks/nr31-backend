package org.nr31.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request object for user authentication")
public class AuthRequest {
    @Schema(description = "User's username", example = "admin")
    @NotBlank(message = "Username is required")
    private String username;

    @Schema(description = "User's password", example = "password123")
    @NotBlank(message = "Password is required")
    private String password;
}
