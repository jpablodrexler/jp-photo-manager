package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.api.exception.InvalidRefreshTokenException;
import com.jpablodrexler.photomanager.domain.entity.RefreshToken;
import com.jpablodrexler.photomanager.domain.entity.User;
import com.jpablodrexler.photomanager.domain.repository.RefreshTokenRepository;
import com.jpablodrexler.photomanager.domain.repository.UserRepository;
import com.jpablodrexler.photomanager.domain.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${photomanager.refresh-token-expiry-days}")
    private int refreshTokenExpiryDays;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public String issueRefreshToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String tokenValue = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Instant now = Instant.now();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(tokenValue);
        refreshToken.setExpiresAt(now.plus(refreshTokenExpiryDays, ChronoUnit.DAYS));
        refreshToken.setRevoked(false);
        refreshToken.setIssuedAt(now);

        refreshTokenRepository.save(refreshToken);
        return tokenValue;
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

        String newTokenValue = issueRefreshToken(username);
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
        refreshTokenRepository.deleteByUser_Id(user.getId());
    }
}
