package com.jpablodrexler.photomanager.domain.service;

import java.time.Instant;

public interface JwtTokenService {

    String generateToken(String username);

    String extractUsername(String token);

    boolean isTokenValid(String token);

    Instant tokenExpiry(String token);
}
