package com.jpablodrexler.photomanager.application.usecase.auth;

import com.jpablodrexler.photomanager.domain.port.in.auth.LoginUseCase;
import com.jpablodrexler.photomanager.domain.port.out.JwtTokenPort;
import com.jpablodrexler.photomanager.domain.port.out.RefreshTokenPort;
import com.jpablodrexler.photomanager.domain.port.out.UserAuthPort;
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
class LoginUseCaseImplTest {

    @Mock
    UserAuthPort userAuthPort;
    @Mock
    JwtTokenPort jwtTokenPort;
    @Mock
    RefreshTokenPort refreshTokenPort;
    @InjectMocks
    LoginUseCaseImpl sut;

    @Test
    void execute_validCredentials_returnsLoginResultWithLowercasedUsername() {
        Instant expiresAt = Instant.parse("2026-08-14T00:00:00Z");
        when(userAuthPort.authenticate("Alice", "secret")).thenReturn("jwt-token");
        when(jwtTokenPort.tokenExpiry("jwt-token")).thenReturn(expiresAt);
        when(refreshTokenPort.issueRefreshToken("Alice", "chrome")).thenReturn("refresh-token-value");

        LoginUseCase.LoginResult result = sut.execute("Alice", "secret", "chrome");

        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.jwtToken()).isEqualTo("jwt-token");
        assertThat(result.jwtExpiresAt()).isEqualTo(expiresAt);
        assertThat(result.refreshTokenValue()).isEqualTo("refresh-token-value");
    }

    @Test
    void execute_validCredentials_issuesRefreshTokenWithGivenUserAgent() {
        when(userAuthPort.authenticate("bob", "secret")).thenReturn("jwt-token");
        when(jwtTokenPort.tokenExpiry("jwt-token")).thenReturn(Instant.now());
        when(refreshTokenPort.issueRefreshToken("bob", "firefox")).thenReturn("refresh-token-value");

        sut.execute("bob", "secret", "firefox");

        verify(refreshTokenPort).issueRefreshToken("bob", "firefox");
    }
}
