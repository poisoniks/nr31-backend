package org.nr31.backend.controller.v1;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.AuthCredentialsDTO;
import org.nr31.backend.dto.AuthRequest;
import org.nr31.backend.dto.AuthResponse;
import org.nr31.backend.dto.ErrorResponse;
import org.nr31.backend.dto.LogoutRequest;
import org.nr31.backend.dto.ValidationErrorResponse;
import org.nr31.backend.service.AuthenticationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.nr31.backend.dto.RefreshTokenRequest;
import org.nr31.backend.dto.RegisterRequest;
import org.nr31.backend.dto.ResendVerificationRequest;
import org.springframework.web.bind.annotation.RequestParam;

import org.nr31.backend.service.RefreshTokenService;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Validated
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user authentication and token management")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final RefreshTokenService refreshTokenService;

    @Operation(summary = "Authenticate user", description = "Validates user credentials and returns JWT access and refresh tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid username or password",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @PostMapping(value = "/login", produces = "application/json", consumes = "application/json")
    public ResponseEntity<AuthResponse> createAuthenticationToken(@Valid @RequestBody AuthRequest authRequest) {
        AuthCredentialsDTO credentialsDTO = authenticationService.authenticate(authRequest.getUsername(),
                authRequest.getPassword());
        return ResponseEntity.ok(new AuthResponse(credentialsDTO.getAccessToken(), credentialsDTO.getRefreshToken()));
    }

    @Operation(summary = "Refresh access token", description = "Generates a new JWT access token using a valid refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Access token successfully refreshed", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @PostMapping(value = "/refresh", produces = "application/json", consumes = "application/json")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String accessToken = refreshTokenService.refreshUserToken(request.getRefreshToken());
        return ResponseEntity.ok(new AuthResponse(accessToken, request.getRefreshToken()));
    }

    @Operation(summary = "Logout user", description = "Invalidates the provided refresh token", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully logged out"),
            @ApiResponse(responseCode = "403", description = "Unauthorized",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @PostMapping(value = "/logout", produces = "application/json", consumes = "application/json")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logoutUser(@Valid @RequestBody LogoutRequest request) {
        refreshTokenService.deleteByToken(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Register a new user", description = "Creates a new user and sends a verification email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully registered"),
            @ApiResponse(responseCode = "409", description = "Username or email already exists",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @PostMapping(value = "/register", produces = "application/json", consumes = "application/json")
    public ResponseEntity<Void> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        authenticationService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Verify user email", description = "Verifies a user's email address using a token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Email successfully verified"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/verify-email")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        authenticationService.verifyEmail(token);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Resend verification email", description = "Resends the verification email if the user exists and is not verified")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verification email resent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ValidationErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email is already verified",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Too many requests - please wait 5 minutes before resending",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/resend-verification", produces = "application/json", consumes = "application/json")
    public ResponseEntity<Void> resendVerificationEmail(@Valid @RequestBody ResendVerificationRequest request) {
        authenticationService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok().build();
    }
}
