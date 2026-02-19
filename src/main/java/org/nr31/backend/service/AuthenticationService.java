package org.nr31.backend.service;

import org.nr31.backend.dto.AuthCredentialsDTO;

public interface AuthenticationService {
    AuthCredentialsDTO authenticate(String username, String password);
}
