package com.jpablodrexler.photomanager.domain.port.in.auth;

import java.time.Instant;

public interface LoginUseCase {

    record LoginResult(String username, String jwtToken, Instant jwtExpiresAt, String refreshTokenValue) {}

    LoginResult execute(String username, String password);
}
