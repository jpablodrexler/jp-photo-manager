package com.jpablodrexler.photomanager.domain.port.in.auth;

import java.time.Instant;

public interface RefreshTokenUseCase {

    record RefreshResult(String username, String jwtToken, Instant jwtExpiresAt, String newRefreshTokenValue) {}

    RefreshResult execute(String refreshTokenValue);
}
