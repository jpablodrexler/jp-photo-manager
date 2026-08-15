package com.jpablodrexler.photomanager.application.usecase.user;

import com.jpablodrexler.photomanager.application.exception.UserNotFoundException;
import com.jpablodrexler.photomanager.domain.model.User;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserUseCaseImplTest {

    @Mock
    UserRepository userRepository;
    @InjectMocks
    GetCurrentUserUseCaseImpl sut;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @BeforeEach
    void setUpAuthenticatedUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("alice", "n/a"));
    }

    @Test
    void execute_authenticatedUserExists_returnsUser() {
        User user = User.builder().id(UUID.randomUUID()).username("alice").build();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        User result = sut.execute();

        assertThat(result).isEqualTo(user);
    }

    @Test
    void execute_authenticatedUserMissingFromRepository_throwsUserNotFoundException() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());

        assertThatThrownBy(sut::execute)
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("alice");
    }
}
