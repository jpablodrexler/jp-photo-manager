package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.model.RefreshToken;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.out.RefreshTokenPort;
import com.jpablodrexler.photomanager.domain.port.out.RefreshTokenRepository;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import com.jpablodrexler.photomanager.infrastructure.web.exception.InvalidRefreshTokenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceAdapterTest {

    @Mock
    RefreshTokenRepository refreshTokenRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    RefreshTokenIssuer refreshTokenIssuer;
    @InjectMocks
    RefreshTokenServiceAdapter sut;

    @Test
    void issueRefreshToken_delegatesToRefreshTokenIssuer() {
        when(refreshTokenIssuer.issueRefreshToken("alice", "chrome")).thenReturn("issued-value");

        String result = sut.issueRefreshToken("alice", "chrome");

        assertThat(result).isEqualTo("issued-value");
    }

    @Test
    void validateAndRotate_tokenNotFound_throwsInvalidRefreshTokenException() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.validateAndRotate("missing"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void validateAndRotate_revokedToken_throwsInvalidRefreshTokenException() {
        RefreshToken revoked = RefreshToken.builder()
                .tokenId(1L)
                .token("revoked-token")
                .revoked(true)
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();
        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> sut.validateAndRotate("revoked-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void validateAndRotate_expiredToken_throwsInvalidRefreshTokenException() {
        RefreshToken expired = RefreshToken.builder()
                .tokenId(1L)
                .token("expired-token")
                .revoked(false)
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();
        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> sut.validateAndRotate("expired-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void validateAndRotate_validToken_revokesOldTokenAndReturnsRotatedToken() {
        User user = User.builder().id(UUID.randomUUID()).username("alice").build();
        RefreshToken existing = RefreshToken.builder()
                .tokenId(1L)
                .token("old-token")
                .user(user)
                .userAgent("chrome")
                .revoked(false)
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();
        Instant newExpiry = Instant.now().plus(30, ChronoUnit.DAYS);
        RefreshToken newToken = RefreshToken.builder()
                .tokenId(2L)
                .token("new-token")
                .expiresAt(newExpiry)
                .build();
        when(refreshTokenRepository.findByToken("old-token")).thenReturn(Optional.of(existing));
        when(refreshTokenIssuer.issueRefreshToken("alice", "chrome")).thenReturn("new-token");
        when(refreshTokenRepository.findByToken("new-token")).thenReturn(Optional.of(newToken));

        RefreshTokenPort.RotatedToken result = sut.validateAndRotate("old-token");

        assertThat(result.newTokenValue()).isEqualTo("new-token");
        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.newExpiresAt()).isEqualTo(newExpiry);
        assertThat(existing.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(existing);
    }

    @Test
    void validateAndRotate_newTokenNotFoundAfterIssue_fallsBackToConfiguredExpiryDays() {
        ReflectionTestUtils.setField(sut, "refreshTokenExpiryDays", 14);
        User user = User.builder().id(UUID.randomUUID()).username("alice").build();
        RefreshToken existing = RefreshToken.builder()
                .tokenId(1L)
                .token("old-token")
                .user(user)
                .userAgent("chrome")
                .revoked(false)
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build();
        when(refreshTokenRepository.findByToken("old-token")).thenReturn(Optional.of(existing));
        when(refreshTokenIssuer.issueRefreshToken("alice", "chrome")).thenReturn("new-token");
        when(refreshTokenRepository.findByToken("new-token")).thenReturn(Optional.empty());

        RefreshTokenPort.RotatedToken result = sut.validateAndRotate("old-token");

        Instant expectedFallback = Instant.now().plus(14, ChronoUnit.DAYS);
        assertThat(result.newExpiresAt()).isCloseTo(expectedFallback, org.assertj.core.api.Assertions.within(5, ChronoUnit.SECONDS));
    }

    @Test
    void revokeAllForUser_userExists_deletesAllTokensForThatUser() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("alice").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        sut.revokeAllForUser("alice");

        verify(refreshTokenRepository).deleteByUserId(userId);
    }

    @Test
    void revokeAllForUser_userNotFound_throwsIllegalArgumentException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.revokeAllForUser("ghost"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ghost");

        verify(refreshTokenRepository, never()).deleteByUserId(any());
    }
}
