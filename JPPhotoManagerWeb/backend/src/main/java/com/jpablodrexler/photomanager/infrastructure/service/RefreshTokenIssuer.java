package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.model.RefreshToken;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.out.RefreshTokenRepository;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenIssuer {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${photomanager.refresh-token-expiry-days}")
    private int refreshTokenExpiryDays;

    private final SecureRandom secureRandom = new SecureRandom();

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
}
