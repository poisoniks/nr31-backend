package org.nr31.backend.service;

import org.nr31.backend.dto.AuthResponse;
import org.nr31.backend.model.RefreshToken;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(String username);

    void deleteByToken(String token);

    boolean isRefreshTokenValid(RefreshToken token);

    AuthResponse refreshUserToken(String token);
}
