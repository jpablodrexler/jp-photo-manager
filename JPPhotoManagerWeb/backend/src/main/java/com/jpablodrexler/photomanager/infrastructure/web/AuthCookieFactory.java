package com.jpablodrexler.photomanager.infrastructure.web;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieFactory {

    private static final String JWT_COOKIE_NAME = "jwt";
    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_COOKIE_PATH = "/api/auth/refresh";

    public ResponseCookie jwtCookie(String token, Duration maxAge) {
        return build(JWT_COOKIE_NAME, token, "/", maxAge);
    }

    public ResponseCookie refreshCookie(String tokenValue, Duration maxAge) {
        return build(REFRESH_COOKIE_NAME, tokenValue, REFRESH_COOKIE_PATH, maxAge);
    }

    public ResponseCookie clearJwtCookie() {
        return jwtCookie("", Duration.ZERO);
    }

    public ResponseCookie clearRefreshCookie() {
        return refreshCookie("", Duration.ZERO);
    }

    private ResponseCookie build(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .path(path)
                .sameSite("Strict")
                .maxAge(maxAge)
                .build();
    }
}
