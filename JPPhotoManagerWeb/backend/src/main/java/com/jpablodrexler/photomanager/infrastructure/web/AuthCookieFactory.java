package com.jpablodrexler.photomanager.infrastructure.web;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieFactory {

    private static final String JWT_COOKIE_NAME = "jwt";
    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    // Must cover every /api/auth/** endpoint that reads this cookie back
    // (refresh, sessions, sessions/{id}) - scoping it to just /api/auth/refresh
    // meant GET/DELETE /api/auth/sessions never received it, so "current
    // session" was always false and "sign out everywhere else" always threw
    // MissingRefreshTokenException. Still narrower than "/" so it isn't sent
    // on unrelated requests.
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

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
