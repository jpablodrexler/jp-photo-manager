package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.port.out.JwtTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JwtTokenAdapter implements JwtTokenPort {

    private final JwtUtil jwtUtil;

    @Override
    public String generateToken(String username) {
        return jwtUtil.generateToken(username);
    }

    @Override
    public String extractUsername(String token) {
        return jwtUtil.extractUsername(token);
    }

    @Override
    public boolean isTokenValid(String token) {
        return jwtUtil.isTokenValid(token);
    }

    @Override
    public Instant tokenExpiry(String token) {
        return jwtUtil.tokenExpiry(token);
    }
}
