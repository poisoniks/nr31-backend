package org.nr31.backend.service;

import org.nr31.backend.model.RefreshToken;
import org.nr31.backend.model.User;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(String username);

    void deleteByToken(String token);

    boolean isRefreshTokenValid(RefreshToken token);

    String refreshUserToken(String token);

    void deleteByUser(User user);
}
