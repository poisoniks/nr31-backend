package org.nr31.backend.service;

import org.nr31.backend.dto.AuthCredentialsDTO;
import org.nr31.backend.dto.RegisterRequest;

public interface AuthenticationService {
    AuthCredentialsDTO authenticate(String username, String password);
    void register(RegisterRequest request);
    void verifyEmail(String token);
}
