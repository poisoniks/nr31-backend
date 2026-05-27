package org.nr31.backend.service;

import org.nr31.backend.dto.auth.AuthCredentialsDTO;
import org.nr31.backend.dto.auth.RegisterRequest;

public interface AuthenticationService {
    AuthCredentialsDTO authenticate(String username, String password);
    void register(RegisterRequest request);
    void verifyEmail(String token);
    void resendVerificationEmail(String email);
    void sendForgotPasswordEmail(String email);
    void resetUserPassword(String token, String newPassword);
}
