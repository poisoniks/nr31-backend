package org.nr31.backend.service;

import org.nr31.backend.dto.AuthRequest;
import org.nr31.backend.dto.AuthResponse;

public interface AuthenticationService {
    AuthResponse authenticate(AuthRequest requestForm);
}
