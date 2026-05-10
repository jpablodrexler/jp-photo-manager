package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.entity.User;
import com.jpablodrexler.photomanager.domain.repository.UserRepository;
import com.jpablodrexler.photomanager.domain.service.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtTokenService jwtUtil;

    @InjectMocks
    UserServiceImpl sut;

    @Test
    void register_newUser_savesWithHashedPassword() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("hashed");

        sut.register("Alice", "secret");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("alice");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void register_duplicateUsername_throwsIllegalArgumentException() {
        User existing = new User();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> sut.register("Alice", "secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticate_validCredentials_returnsToken() {
        User user = new User();
        user.setUsername("alice");
        user.setPasswordHash("hashed");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken("alice")).thenReturn("jwt-token");

        String token = sut.authenticate("Alice", "secret");

        assertThat(token).isEqualTo("jwt-token");
    }

    @Test
    void authenticate_invalidPassword_throwsBadCredentialsException() {
        User user = new User();
        user.setUsername("alice");
        user.setPasswordHash("hashed");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> sut.authenticate("alice", "wrong"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void authenticate_unknownUsername_throwsBadCredentialsException() {
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.authenticate("unknown", "secret"))
                .isInstanceOf(BadCredentialsException.class);
    }
}
