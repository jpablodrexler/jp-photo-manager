package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.infrastructure.web.exception.InvalidRefreshTokenException;
import com.jpablodrexler.photomanager.domain.model.RefreshToken;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.out.RefreshTokenRepository;
import com.jpablodrexler.photomanager.domain.port.out.RefreshTokenPort;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceAdapter implements RefreshTokenPort {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenIssuer refreshTokenIssuer;

    @Value("${photomanager.refresh-token-expiry-days}")
    private int refreshTokenExpiryDays;

    @Override
    public String issueRefreshToken(String username) {
        return refreshTokenIssuer.issueRefreshToken(username);
    }

    @Override
    @Transactional
    public RotatedToken validateAndRotate(String tokenValue) {
        RefreshToken existing = refreshTokenRepository.findByToken(tokenValue)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (existing.isRevoked() || existing.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException();
        }

        String username = existing.getUser().getUsername();
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        String newTokenValue = refreshTokenIssuer.issueRefreshToken(username);
        Instant newExpiresAt = refreshTokenRepository.findByToken(newTokenValue)
                .map(RefreshToken::getExpiresAt)
                .orElse(Instant.now().plus(refreshTokenExpiryDays, ChronoUnit.DAYS));

        return new RotatedToken(newTokenValue, username, newExpiresAt);
    }

    @Override
    @Transactional
    public void revokeAllForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
        refreshTokenRepository.deleteByUserId(user.getId());
    }
}
