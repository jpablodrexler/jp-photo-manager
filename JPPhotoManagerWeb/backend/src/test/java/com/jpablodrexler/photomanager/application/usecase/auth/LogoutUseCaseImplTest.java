package com.jpablodrexler.photomanager.application.usecase.auth;

import com.jpablodrexler.photomanager.domain.port.out.JwtTokenPort;
import com.jpablodrexler.photomanager.domain.port.out.RefreshTokenPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogoutUseCaseImplTest {

    @Mock
    JwtTokenPort jwtTokenPort;
    @Mock
    RefreshTokenPort refreshTokenPort;
    @InjectMocks
    LogoutUseCaseImpl sut;

    @Test
    void execute_nullToken_doesNothing() {
        sut.execute(null);

        verifyNoInteractions(jwtTokenPort, refreshTokenPort);
    }

    @Test
    void execute_tokenExtractionFails_returnsWithoutRevoking() {
        when(jwtTokenPort.extractUsername("bad-token")).thenThrow(new RuntimeException("malformed"));

        sut.execute("bad-token");

        verify(refreshTokenPort, never()).revokeAllForUser(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void execute_validToken_revokesAllTokensForExtractedUser() {
        when(jwtTokenPort.extractUsername("good-token")).thenReturn("alice");

        sut.execute("good-token");

        verify(refreshTokenPort).revokeAllForUser("alice");
    }

    @Test
    void execute_revokeThrows_doesNotPropagateException() {
        when(jwtTokenPort.extractUsername("good-token")).thenReturn("alice");
        org.mockito.Mockito.doThrow(new RuntimeException("db down"))
                .when(refreshTokenPort).revokeAllForUser("alice");

        assertThatCode(() -> sut.execute("good-token")).doesNotThrowAnyException();
    }
}
