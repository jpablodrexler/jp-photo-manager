package com.jpablodrexler.photomanager.application.usecase.auth;

import com.jpablodrexler.photomanager.domain.port.in.auth.RefreshTokenUseCase;
import com.jpablodrexler.photomanager.domain.port.out.JwtTokenPort;
import com.jpablodrexler.photomanager.domain.port.out.RefreshTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCaseImpl implements RefreshTokenUseCase {

    private final RefreshTokenPort refreshTokenPort;
    private final JwtTokenPort jwtTokenPort;

    @Override
    @Transactional
    public RefreshResult execute(String refreshTokenValue) {
        RefreshTokenPort.RotatedToken rotated = refreshTokenPort.validateAndRotate(refreshTokenValue);
        String newJwt = jwtTokenPort.generateToken(rotated.username());
        Instant newExpiresAt = jwtTokenPort.tokenExpiry(newJwt);
        return new RefreshResult(rotated.username(), newJwt, newExpiresAt, rotated.newTokenValue());
    }
}
