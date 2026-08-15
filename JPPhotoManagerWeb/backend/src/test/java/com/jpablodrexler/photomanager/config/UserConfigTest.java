package com.jpablodrexler.photomanager.config;

import com.jpablodrexler.photomanager.domain.model.User;
import com.jpablodrexler.photomanager.domain.port.out.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserConfigTest {

    @Mock UserRepository userRepository;
    @InjectMocks UserConfig sut;

    @Test
    void passwordEncoder_encodesAndMatches() {
        BCryptPasswordEncoder encoder = sut.passwordEncoder();

        String encoded = encoder.encode("secret");

        assertThat(encoder.matches("secret", encoded)).isTrue();
        assertThat(encoder.matches("wrong", encoded)).isFalse();
    }

    @Test
    void userDetailsService_existingUser_returnsMappedUserDetails() {
        User user = new User();
        user.setUsername("jdoe");
        user.setPasswordHash("hashed-password");
        user.setRole("ADMIN");
        when(userRepository.findByUsername("jdoe")).thenReturn(Optional.of(user));

        UserDetailsService service = sut.userDetailsService();
        UserDetails details = service.loadUserByUsername("jdoe");

        assertThat(details.getUsername()).isEqualTo("jdoe");
        assertThat(details.getPassword()).isEqualTo("hashed-password");
        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void userDetailsService_unknownUser_throwsUsernameNotFoundException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        UserDetailsService service = sut.userDetailsService();

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost");
    }
}
