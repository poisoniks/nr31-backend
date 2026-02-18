package org.nr31.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.nr31.backend.dto.ErrorResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType("application/json");

        ErrorResponse errorResponse;

        if (authException instanceof InsufficientAuthenticationException) {
            errorResponse = new ErrorResponse("Insufficient authentication", LocalDateTime.now());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            errorResponse = new ErrorResponse("Access denied", LocalDateTime.now());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
