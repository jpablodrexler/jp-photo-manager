package com.jpablodrexler.photomanager.application.usecase.auth;

import com.jpablodrexler.photomanager.domain.port.in.auth.LogoutUseCase;
import com.jpablodrexler.photomanager.domain.port.out.JwtTokenPort;
import com.jpablodrexler.photomanager.domain.port.out.RefreshTokenPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogoutUseCaseImpl implements LogoutUseCase {

    private final JwtTokenPort jwtTokenPort;
    private final RefreshTokenPort refreshTokenPort;

    @Override
    @Transactional
    public void execute(String jwtToken) {
        if (jwtToken == null) {
            return;
        }
        String username;
        try {
            username = jwtTokenPort.extractUsername(jwtToken);
        } catch (Exception e) {
            return;
        }
        try {
            refreshTokenPort.revokeAllForUser(username);
        } catch (Exception e) {
            log.warn("Failed to revoke refresh tokens for user {} during logout", username, e);
        }
    }
}
