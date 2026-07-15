package com.jpablodrexler.photomanager.domain.port.out;

import java.time.Instant;

public interface RefreshTokenPort {

    record RotatedToken(String newTokenValue, String username, Instant newExpiresAt) {}

    String issueRefreshToken(String username);

    RotatedToken validateAndRotate(String tokenValue);

    void revokeAllForUser(String username);
}
