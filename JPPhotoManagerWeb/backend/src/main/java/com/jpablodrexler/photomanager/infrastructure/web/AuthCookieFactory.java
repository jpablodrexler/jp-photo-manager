package com.jpablodrexler.photomanager.infrastructure.web;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieFactory {

    private static final String JWT_COOKIE_NAME = "jwt";
    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    // Scoped to /api/auth rather than just /api/auth/refresh: AuthController's
    // GET /api/auth/sessions and DELETE /api/auth/sessions[/{id}] also need this cookie to
    // identify "the current session" (see GetActiveSessionsUseCaseImpl/RevokeSessionUseCase),
    // and a narrower path silently withholds the cookie from those requests -- confirmed for
    // real via release-e2e-suite: the "This device" badge never appeared because the browser
    // never even sent the cookie to /api/auth/sessions, and the same gap meant "sign out
    // everywhere else" couldn't identify which session to keep either.
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
