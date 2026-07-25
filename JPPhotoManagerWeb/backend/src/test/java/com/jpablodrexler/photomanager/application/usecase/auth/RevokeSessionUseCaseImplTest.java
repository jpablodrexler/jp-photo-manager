package com.jpablodrexler.photomanager.application.usecase.auth;

import com.jpablodrexler.photomanager.application.exception.MissingRefreshTokenException;
import com.jpablodrexler.photomanager.application.exception.SessionNotFoundException;
import com.jpablodrexler.photomanager.domain.model.RefreshToken;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.out.RefreshTokenRepository;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevokeSessionUseCaseImplTest {

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    RevokeSessionUseCaseImpl sut;

    private final UUID userId = UUID.randomUUID();
    private final UUID otherUserId = UUID.randomUUID();

    @BeforeEach
    void setUpAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null, List.of()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private User buildUser(UUID id) {
        return User.builder().id(id).username("alice").build();
    }

    private RefreshToken buildToken(long tokenId, UUID ownerId, String token) {
        return RefreshToken.builder().tokenId(tokenId).user(buildUser(ownerId)).token(token).build();
    }

    @Test
    void revokeOne_ownSession_deletesById() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(buildUser(userId)));
        when(refreshTokenRepository.findById(1L)).thenReturn(Optional.of(buildToken(1L, userId, "tok")));

        sut.revokeOne(1L);

        verify(refreshTokenRepository).deleteById(1L);
    }

    @Test
    void revokeOne_sessionBelongsToDifferentUser_throwsSessionNotFoundExceptionAndDoesNotDelete() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(buildUser(userId)));
        when(refreshTokenRepository.findById(2L)).thenReturn(Optional.of(buildToken(2L, otherUserId, "tok")));

        assertThatThrownBy(() -> sut.revokeOne(2L)).isInstanceOf(SessionNotFoundException.class);
        verify(refreshTokenRepository, never()).deleteById(any());
    }

    @Test
    void revokeOne_sessionDoesNotExist_throwsSessionNotFoundException() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(buildUser(userId)));
        when(refreshTokenRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.revokeOne(99L)).isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void revokeAllOthers_nullCurrentRefreshTokenValue_throwsMissingRefreshTokenExceptionAndDeletesNothing() {
        assertThatThrownBy(() -> sut.revokeAllOthers(null)).isInstanceOf(MissingRefreshTokenException.class);

        verify(refreshTokenRepository, never()).deleteById(any());
        verify(refreshTokenRepository, never()).findActiveByUserId(any(), any());
    }

    @Test
    void revokeAllOthers_blankCurrentRefreshTokenValue_throwsMissingRefreshTokenExceptionAndDeletesNothing() {
        assertThatThrownBy(() -> sut.revokeAllOthers("  ")).isInstanceOf(MissingRefreshTokenException.class);

        verify(refreshTokenRepository, never()).deleteById(any());
        verify(refreshTokenRepository, never()).findActiveByUserId(any(), any());
    }

    @Test
    void revokeAllOthers_deletesAllTokensExceptTheOneMatchingCurrentRefreshTokenValue() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(buildUser(userId)));
        RefreshToken current = buildToken(1L, userId, "current-tok");
        RefreshToken other1 = buildToken(2L, userId, "other-tok-1");
        RefreshToken other2 = buildToken(3L, userId, "other-tok-2");
        when(refreshTokenRepository.findActiveByUserId(eq(userId), any(Instant.class)))
                .thenReturn(List.of(current, other1, other2));

        sut.revokeAllOthers("current-tok");

        verify(refreshTokenRepository).deleteById(2L);
        verify(refreshTokenRepository).deleteById(3L);
        verify(refreshTokenRepository, never()).deleteById(1L);
    }
}
