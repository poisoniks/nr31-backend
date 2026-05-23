package org.nr31.backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.dto.AppConfigDto;
import org.nr31.backend.dto.AuthCredentialsDTO;
import org.nr31.backend.dto.RegisterRequest;
import org.nr31.backend.dto.ErrorCode;
import org.nr31.backend.exception.ConflictException;
import org.nr31.backend.model.RefreshToken;
import org.nr31.backend.model.User;
import org.nr31.backend.model.EmailVerificationToken;
import org.nr31.backend.repository.UserRepository;
import org.nr31.backend.repository.EmailVerificationTokenRepository;
import org.nr31.backend.security.JwtUtil;
import org.nr31.backend.service.AuthenticationService;
import org.nr31.backend.service.RefreshTokenService;
import org.nr31.backend.service.EmailSenderService;
import org.nr31.backend.service.AppConfigService;
import org.nr31.backend.model.AppConfigKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.i18n.LocaleContextHolder;
import tools.jackson.databind.JsonNode;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtAuthenticationService implements AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailSenderService emailSenderService;
    private final AppConfigService appConfigService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public AuthCredentialsDTO authenticate(String username, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));

        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            log.debug("User not found during authentication: {}", username);
            throw new BadCredentialsException("No such user: " + username, e);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No such user: " + username));

        if (!user.isEmailVerified() && isBlockUnverifiedUsersEnabled()) {
            log.debug("Authentication failed: email not verified for user: {}", username);
            throw new DisabledException("Email is not verified");
        }

        String jwt = jwtUtil.generateToken(userDetails);

        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername());

        return new AuthCredentialsDTO(jwt, refreshToken.getToken());
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username is already taken", ErrorCode.CONFLICT);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already registered", ErrorCode.CONFLICT);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .user(savedUser)
                .expiryDate(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();

        emailVerificationTokenRepository.save(verificationToken);

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", savedUser.getUsername());
        variables.put("verificationUrl", frontendUrl + "/verify-email?token=" + token);

        emailSenderService.sendHtmlEmail(
                savedUser.getEmail(),
                "email.verify.subject",
                "email-verification",
                variables,
                LocaleContextHolder.getLocale()
        );
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email verification token"));

        if (verificationToken.getExpiryDate().isBefore(Instant.now())) {
            emailVerificationTokenRepository.deleteByTokenCustom(token);
            throw new IllegalArgumentException("Verification token has expired");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.saveAndFlush(user);

        emailVerificationTokenRepository.deleteByTokenCustom(token);
    }

    private boolean isBlockUnverifiedUsersEnabled() {
        try {
            AppConfigDto config = appConfigService.getConfig(AppConfigKey.FEATURE_SWITCHES);
            JsonNode configNode = config.getConfigValue();
            if (configNode != null && configNode.isArray()) {
                for (JsonNode element : configNode) {
                    if (element.has("name") && "block_unverified_users".equals(element.get("name").asText())) {
                        JsonNode enabledNode = element.get("enabled");
                        if (enabledNode != null) {
                            return enabledNode.asBoolean();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to check block_unverified_users feature switch", e);
        }
        return false;
    }
}
