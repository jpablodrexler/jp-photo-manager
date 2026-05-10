package com.jpablodrexler.photomanager.domain.service;

import java.time.Instant;

public interface RefreshTokenService {

    record RotatedToken(String newTokenValue, String username, Instant newExpiresAt) {}

    String issueRefreshToken(String username);

    RotatedToken validateAndRotate(String tokenValue);

    void revokeAllForUser(String username);
}
