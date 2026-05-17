package com.jpablodrexler.photomanager.infrastructure.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hmac-sha256-signing";
    private static final int EXPIRY_HOURS = 1;

    private JwtUtil sut;

    @BeforeEach
    void setUp() {
        sut = new JwtUtil();
        ReflectionTestUtils.setField(sut, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(sut, "jwtExpiryHours", EXPIRY_HOURS);
        sut.init();
    }

    @Test
    void init_blankSecret_throwsIllegalStateException() {
        JwtUtil util = new JwtUtil();
        ReflectionTestUtils.setField(util, "jwtSecret", "");
        ReflectionTestUtils.setField(util, "jwtExpiryHours", 1);

        assertThatThrownBy(util::init).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void generateToken_returnsNonBlankToken() {
        String token = sut.generateToken("alice");
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUsername_returnsCorrectUsername() {
        String token = sut.generateToken("alice");

        assertThat(sut.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void tokenExpiry_returnsFutureInstant() {
        String token = sut.generateToken("alice");

        Instant expiry = sut.tokenExpiry(token);

        assertThat(expiry).isAfter(Instant.now());
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = sut.generateToken("alice");
        assertThat(sut.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_tamperedToken_returnsFalse() {
        assertThat(sut.isTokenValid("not.a.valid.jwt")).isFalse();
    }

    @Test
    void isTokenValid_tokenSignedWithDifferentKey_returnsFalse() {
        JwtUtil other = new JwtUtil();
        ReflectionTestUtils.setField(other, "jwtSecret", "different-secret-key-that-is-long-enough-for-hmac");
        ReflectionTestUtils.setField(other, "jwtExpiryHours", 1);
        other.init();
        String token = other.generateToken("alice");

        assertThat(sut.isTokenValid(token)).isFalse();
    }
}
