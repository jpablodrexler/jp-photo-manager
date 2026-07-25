package com.jpablodrexler.photomanager.application.usecase.auth;

import com.jpablodrexler.photomanager.application.exception.UserNotFoundException;
import com.jpablodrexler.photomanager.domain.model.RefreshToken;
import com.jpablodrexler.photomanager.domain.model.SessionInfo;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetActiveSessionsUseCaseImplTest {

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    GetActiveSessionsUseCaseImpl sut;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUpAuthentication() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", null, List.of()));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private User buildUser() {
        return User.builder().id(userId).username("alice").build();
    }

    private RefreshToken buildToken(long tokenId, String token, boolean revoked, Instant expiresAt,
                                     Instant lastUsedAt, String userAgent) {
        return RefreshToken.builder()
                .tokenId(tokenId)
                .user(buildUser())
                .token(token)
                .revoked(revoked)
                .expiresAt(expiresAt)
                .issuedAt(lastUsedAt)
                .lastUsedAt(lastUsedAt)
                .userAgent(userAgent)
                .build();
    }

    @Test
    void execute_delegatesNonExpiredNonRevokedFilteringToRepositoryQuery() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(buildUser()));
        // findActiveByUserId is the repository method that filters out expired/revoked tokens at the
        // DB level (see RefreshTokenRepository.findActiveByUserId) — only the already-filtered
        // "active" token is stubbed here, simulating what the query actually returns.
        RefreshToken active = buildToken(1L, "tok-active", false, Instant.now().plusSeconds(3600), Instant.now(),
                "Mozilla/5.0 Windows Chrome/1");
        when(refreshTokenRepository.findActiveByUserId(eq(userId), any())).thenReturn(List.of(active));

        List<SessionInfo> result = sut.execute(null);

        assertThat(result).extracting(SessionInfo::id).containsExactly(1L);
        verify(refreshTokenRepository).findActiveByUserId(eq(userId), any(Instant.class));
    }

    @Test
    void execute_currentTokenMatchesCookieValue_marksThatSessionCurrent() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(buildUser()));
        RefreshToken tokenA = buildToken(1L, "tok-a", false, Instant.now().plusSeconds(3600), Instant.now(),
                "agent-a");
        RefreshToken tokenB = buildToken(2L, "tok-b", false, Instant.now().plusSeconds(3600), Instant.now(),
                "agent-b");
        when(refreshTokenRepository.findActiveByUserId(eq(userId), any())).thenReturn(List.of(tokenA, tokenB));

        List<SessionInfo> result = sut.execute("tok-b");

        assertThat(result)
                .extracting(SessionInfo::id, SessionInfo::current)
                .containsExactlyInAnyOrder(tuple(1L, false), tuple(2L, true));
    }

    @Test
    void execute_unknownUser_throwsUserNotFoundException() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.execute(null)).isInstanceOf(UserNotFoundException.class);
    }
}
