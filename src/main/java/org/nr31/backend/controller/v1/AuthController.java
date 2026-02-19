package org.nr31.backend.controller.v1;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.AuthCredentialsDTO;
import org.nr31.backend.dto.AuthRequest;
import org.nr31.backend.dto.AuthResponse;
import org.nr31.backend.dto.LogoutRequest;
import org.nr31.backend.service.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.nr31.backend.dto.RefreshTokenRequest;

import org.nr31.backend.service.RefreshTokenService;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> createAuthenticationToken(@Valid @RequestBody AuthRequest authRequest) {
        AuthCredentialsDTO credentialsDTO = authenticationService.authenticate(authRequest.getUsername(),
                authRequest.getPassword());
        return ResponseEntity.ok(new AuthResponse(credentialsDTO.getAccessToken(), credentialsDTO.getRefreshToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String accessToken = refreshTokenService.refreshUserToken(request.getRefreshToken());
        return ResponseEntity.ok(new AuthResponse(accessToken, request.getRefreshToken()));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> logoutUser(@Valid @RequestBody LogoutRequest request) {
        refreshTokenService.deleteByToken(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }
}
