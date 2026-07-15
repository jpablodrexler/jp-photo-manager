package com.jpablodrexler.photomanager.infrastructure.service;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenIssuerTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenIssuer sut;

    private User buildUser(String username) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        return user;
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
    void issueRefreshToken_unknownUser_throwsIllegalArgumentException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.issueRefreshToken("ghost"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
