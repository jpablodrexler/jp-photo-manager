package com.jpablodrexler.photomanager.application.usecase.auth;

import com.jpablodrexler.photomanager.domain.port.in.auth.RefreshTokenUseCase;
import com.jpablodrexler.photomanager.domain.port.out.JwtTokenPort;
import com.jpablodrexler.photomanager.domain.port.out.RefreshTokenPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenUseCaseImplTest {

    @Mock
    RefreshTokenPort refreshTokenPort;
    @Mock
    JwtTokenPort jwtTokenPort;
    @InjectMocks
    RefreshTokenUseCaseImpl sut;

    @Test
    void execute_validRefreshToken_rotatesTokenAndReturnsNewJwtWithItsOwnExpiry() {
        Instant rotatedExpiry = Instant.parse("2026-09-01T00:00:00Z");
        Instant jwtExpiry = Instant.parse("2026-08-14T01:00:00Z");
        RefreshTokenPort.RotatedToken rotated =
                new RefreshTokenPort.RotatedToken("new-refresh-value", "alice", rotatedExpiry);
        when(refreshTokenPort.validateAndRotate("old-refresh-value")).thenReturn(rotated);
        when(jwtTokenPort.generateToken("alice")).thenReturn("new-jwt");
        when(jwtTokenPort.tokenExpiry("new-jwt")).thenReturn(jwtExpiry);

        RefreshTokenUseCase.RefreshResult result = sut.execute("old-refresh-value");

        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.jwtToken()).isEqualTo("new-jwt");
        assertThat(result.newRefreshTokenValue()).isEqualTo("new-refresh-value");
        // jwtExpiresAt comes from the JWT port's own expiry, not the rotated token's expiry.
        assertThat(result.jwtExpiresAt()).isEqualTo(jwtExpiry);
    }

    @Test
    void execute_validRefreshToken_generatesJwtForRotatedUsername() {
        RefreshTokenPort.RotatedToken rotated =
                new RefreshTokenPort.RotatedToken("new-refresh-value", "bob", Instant.now());
        when(refreshTokenPort.validateAndRotate("old-value")).thenReturn(rotated);
        when(jwtTokenPort.generateToken("bob")).thenReturn("jwt-for-bob");
        when(jwtTokenPort.tokenExpiry("jwt-for-bob")).thenReturn(Instant.now());

        sut.execute("old-value");

        verify(jwtTokenPort).generateToken("bob");
    }
}
