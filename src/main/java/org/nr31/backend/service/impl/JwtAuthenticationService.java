package org.nr31.backend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nr31.backend.dto.AuthRequest;
import org.nr31.backend.dto.AuthResponse;
import org.nr31.backend.security.JwtUtil;
import org.nr31.backend.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtAuthenticationService implements AuthenticationService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public AuthResponse authenticate(AuthRequest requestForm) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(requestForm.getUsername(), requestForm.getPassword()));

        UserDetails userDetails;
        try {
             userDetails = userDetailsService.loadUserByUsername(requestForm.getUsername());
        } catch (UsernameNotFoundException e) {
            log.debug("User not found during authentication: {}", requestForm.getUsername());
            throw new BadCredentialsException("No such user: " + requestForm.getUsername(), e);
        }

        String jwt = jwtUtil.generateToken(userDetails);

        return new AuthResponse(jwt);
    }
}
