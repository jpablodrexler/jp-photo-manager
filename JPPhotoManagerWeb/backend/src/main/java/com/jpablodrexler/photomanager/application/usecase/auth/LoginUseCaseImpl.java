package com.jpablodrexler.photomanager.application.usecase.auth;

import com.jpablodrexler.photomanager.domain.port.in.auth.LoginUseCase;
import com.jpablodrexler.photomanager.domain.port.out.JwtTokenPort;
import com.jpablodrexler.photomanager.domain.port.out.RefreshTokenPort;
import com.jpablodrexler.photomanager.domain.port.out.UserAuthPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LoginUseCaseImpl implements LoginUseCase {

    private final UserAuthPort userAuthPort;
    private final JwtTokenPort jwtTokenPort;
    private final RefreshTokenPort refreshTokenPort;

    @Override
    @Transactional
    public LoginResult execute(String username, String password) {
        String token = userAuthPort.authenticate(username, password);
        Instant expiresAt = jwtTokenPort.tokenExpiry(token);
        String refreshTokenValue = refreshTokenPort.issueRefreshToken(username);
        return new LoginResult(username.toLowerCase(), token, expiresAt, refreshTokenValue);
    }
}
