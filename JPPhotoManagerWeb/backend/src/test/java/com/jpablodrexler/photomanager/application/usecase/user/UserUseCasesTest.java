package com.jpablodrexler.photomanager.application.usecase.user;

import com.jpablodrexler.photomanager.domain.model.UserSummary;
import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserUseCasesTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    class CreateUserUseCaseImplTest {

        @Mock UserRepository userRepository;
        @Mock PasswordEncoder passwordEncoder;
        @InjectMocks CreateUserUseCaseImpl sut;

        @Test
        void execute_usernameTaken_throwsIllegalArgumentException() {
            User existing = User.builder().id(UUID.randomUUID()).username("alice").build();
            when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> sut.execute("Alice", "pass", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("alice");
            verify(userRepository, never()).save(any());
        }

        @Test
        void execute_createsUserWithoutRole() {
            when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("pass")).thenReturn("hashed");
            User saved = User.builder().id(UUID.randomUUID()).username("alice").createdAt(Instant.now()).build();
            when(userRepository.save(any())).thenReturn(saved);

            UserSummary result = sut.execute("Alice", "pass", null);

            assertThat(result.username()).isEqualTo("alice");
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
            assertThat(captor.getValue().getRole()).isNull();
        }

        @Test
        void execute_createsUserWithRole() {
            when(userRepository.findByUsername("bob")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            User saved = User.builder().id(UUID.randomUUID()).username("bob").role("ADMIN").createdAt(Instant.now()).build();
            when(userRepository.save(any())).thenReturn(saved);

            sut.execute("Bob", "pass", "ADMIN");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getRole()).isEqualTo("ADMIN");
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class UpdatePasswordUseCaseImplTest {

        @Mock UserRepository userRepository;
        @Mock PasswordEncoder passwordEncoder;
        @InjectMocks UpdatePasswordUseCaseImpl sut;

        @Test
        void execute_userNotFound_throwsEntityNotFoundException() {
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.execute(userId, "newpass"))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        void execute_userFound_updatesPasswordHash() {
            UUID userId = UUID.randomUUID();
            User user = User.builder().id(userId).username("alice").build();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("newpass")).thenReturn("newhash");
            when(userRepository.save(any())).thenReturn(user);

            sut.execute(userId, "newpass");

            assertThat(user.getPasswordHash()).isEqualTo("newhash");
            verify(userRepository).save(user);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class DeleteUserUseCaseImplTest {

        @Mock UserRepository userRepository;
        @InjectMocks DeleteUserUseCaseImpl sut;

        @Test
        void execute_userNotFound_throwsEntityNotFoundException() {
            UUID userId = UUID.randomUUID();
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> sut.execute(userId))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        void execute_userFound_deletesById() {
            UUID userId = UUID.randomUUID();
            User user = User.builder().id(userId).username("alice").build();
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            sut.execute(userId);

            verify(userRepository).deleteById(userId);
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class ListUsersUseCaseImplTest {

        @Mock UserRepository userRepository;
        @InjectMocks ListUsersUseCaseImpl sut;

        @Test
        void execute_returnsMappedUserSummaries() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            Instant now = Instant.now();
            when(userRepository.findAll()).thenReturn(List.of(
                    User.builder().id(id1).username("alice").createdAt(now).build(),
                    User.builder().id(id2).username("bob").createdAt(now).build()));

            List<UserSummary> result = sut.execute();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).username()).isEqualTo("alice");
            assertThat(result.get(1).username()).isEqualTo("bob");
        }
    }
}
