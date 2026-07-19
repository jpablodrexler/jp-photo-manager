package com.jpablodrexler.photomanager.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCookieFactoryTest {

    private final AuthCookieFactory sut = new AuthCookieFactory();

    @Test
    void jwtCookie_returnsHttpOnlyCookieOnRootPath() {
        ResponseCookie cookie = sut.jwtCookie("token-value", Duration.ofMinutes(15));

        assertThat(cookie.getName()).isEqualTo("jwt");
        assertThat(cookie.getValue()).isEqualTo("token-value");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void refreshCookie_returnsHttpOnlyCookieScopedToRefreshEndpoint() {
        ResponseCookie cookie = sut.refreshCookie("refresh-value", Duration.ofDays(30));

        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.getValue()).isEqualTo("refresh-value");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/auth/refresh");
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(30));
    }

    @Test
    void clearJwtCookie_returnsEmptyValueWithZeroMaxAge() {
        ResponseCookie cookie = sut.clearJwtCookie();

        assertThat(cookie.getName()).isEqualTo("jwt");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
    }

    @Test
    void clearRefreshCookie_returnsEmptyValueWithZeroMaxAge() {
        ResponseCookie cookie = sut.clearRefreshCookie();

        assertThat(cookie.getName()).isEqualTo("refreshToken");
        assertThat(cookie.getValue()).isEmpty();
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ZERO);
    }
}
