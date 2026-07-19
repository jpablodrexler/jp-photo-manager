package com.jpablodrexler.photomanager.domain.port.out;

import java.time.Instant;

public interface JwtTokenPort {

    String generateToken(String username);

    String extractUsername(String token);

    boolean isTokenValid(String token);

    Instant tokenExpiry(String token);
}
