package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.infrastructure.web.exception.InvalidRefreshTokenException;
import com.jpablodrexler.photomanager.domain.model.RefreshToken;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.out.RefreshTokenRepository;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenServiceImpl sut;

    private User buildUser(String username) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        return user;
    }

    private RefreshToken buildValidToken(User user) {
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setToken("existing-token-value");
        rt.setExpiresAt(Instant.now().plusSeconds(3600));
        rt.setRevoked(false);
        rt.setIssuedAt(Instant.now());
        return rt;
    }

    @Test
    void issueRefreshToken_validUser_savesTokenAndReturnsNonBlankString() {
        ReflectionTestUtils.setField(sut, "refreshTokenExpiryDays", 30);
        User user = buildUser("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String result = sut.issueRefreshToken("alice");

        assertThat(result).isNotBlank();
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void validateAndRotate_validToken_marksOldRevokedAndReturnsRotatedToken() {
        ReflectionTestUtils.setField(sut, "refreshTokenExpiryDays", 30);
        User user = buildUser("alice");
        RefreshToken existing = buildValidToken(user);
        RefreshToken newToken = new RefreshToken();
        newToken.setToken("new-token");
        newToken.setExpiresAt(Instant.now().plusSeconds(86400));

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByToken("existing-token-value")).thenReturn(Optional.of(existing));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(refreshTokenRepository.findByToken(argThat(t -> !"existing-token-value".equals(t))))
                .thenReturn(Optional.of(newToken));

        RefreshTokenServiceImpl.RotatedToken result = sut.validateAndRotate("existing-token-value");

        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.newExpiresAt()).isNotNull();
        assertThat(existing.isRevoked()).isTrue();
    }

    @Test
    void validateAndRotate_revokedToken_throwsInvalidRefreshTokenException() {
        User user = buildUser("alice");
        RefreshToken revoked = buildValidToken(user);
        revoked.setRevoked(true);
        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> sut.validateAndRotate("revoked-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void validateAndRotate_expiredToken_throwsInvalidRefreshTokenException() {
        User user = buildUser("alice");
        RefreshToken expired = buildValidToken(user);
        expired.setExpiresAt(Instant.now().minusSeconds(1));
        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> sut.validateAndRotate("expired-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void validateAndRotate_unknownToken_throwsInvalidRefreshTokenException() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.validateAndRotate("unknown"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
