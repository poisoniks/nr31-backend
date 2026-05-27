package org.nr31.backend.service.impl;

import lombok.RequiredArgsConstructor;

import org.nr31.backend.exception.TokenRefreshException;
import org.nr31.backend.model.RefreshToken;
import org.nr31.backend.model.User;
import org.nr31.backend.repository.RefreshTokenRepository;
import org.nr31.backend.repository.UserRepository;
import org.nr31.backend.security.JwtUtil;
import org.nr31.backend.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Value("${jwt.refresh.expiration}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    public RefreshToken createRefreshToken(String username) {
        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUser(userRepository.findByUsername(username)
                .orElseThrow(() -> new TokenRefreshException("User not found during refresh token creation")));
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());

        refreshToken = refreshTokenRepository.save(refreshToken);
        return refreshToken;
    }

    @Override
    public boolean isRefreshTokenValid(RefreshToken token) {
        return token.getExpiryDate().compareTo(Instant.now()) >= 0;
    }

    @Override
    @Transactional
    public void deleteByToken(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }

        refreshTokenRepository.findByToken(token).ifPresent(refreshTokenRepository::delete);
    }

    @Override
    public String refreshUserToken(String refreshToken) {
        return refreshTokenRepository.findByToken(refreshToken)
                .filter(this::isRefreshTokenValid)
                .map(RefreshToken::getUser)
                .map(user -> jwtUtil.generateToken(userDetailsService.loadUserByUsername(user.getUsername())))
                .orElseThrow(() -> new TokenRefreshException("Refresh token is expired or not found"));
    }

    @Override
    @Transactional
    public void deleteByUser(User user) {
        if (user == null) {
            return;
        }
        refreshTokenRepository.deleteByUser(user);
    }
}
