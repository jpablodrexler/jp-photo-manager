package com.jpablodrexler.photomanager.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the two plain bean factory methods on this {@code @Profile("test")} config that don't
 * require a live {@code HttpSecurity}/{@code ApplicationContext} to exercise. {@code
 * testSecurityFilterChain(HttpSecurity)} itself is not unit-testable in isolation — building a
 * real {@code HttpSecurity} needs a full Spring Security application context — and is exercised
 * indirectly by every {@code @SpringBootTest}/{@code @WebMvcTest} that runs under the "test"
 * profile.
 */
class TestSecurityConfigTest {

    TestSecurityConfig sut = new TestSecurityConfig();

    @Test
    void passwordEncoder_encodesAndMatches() {
        BCryptPasswordEncoder encoder = sut.passwordEncoder();

        String encoded = encoder.encode("secret");

        assertThat(encoder.matches("secret", encoded)).isTrue();
    }

    @Test
    void userDetailsService_anyUsername_returnsUserWithUserRole() {
        UserDetailsService service = sut.userDetailsService();

        UserDetails details = service.loadUserByUsername("anyone");

        assertThat(details.getUsername()).isEqualTo("anyone");
        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
    }
}
