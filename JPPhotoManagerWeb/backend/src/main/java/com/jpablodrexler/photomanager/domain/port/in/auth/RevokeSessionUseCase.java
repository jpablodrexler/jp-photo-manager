package com.jpablodrexler.photomanager.domain.port.in.auth;

public interface RevokeSessionUseCase {

    /**
     * Revokes a single session belonging to the authenticated user. Throws
     * {@link com.jpablodrexler.photomanager.application.exception.SessionNotFoundException} if no
     * session with {@code sessionId} exists for the current user (including when it belongs to a
     * different user, to avoid leaking existence of other users' sessions).
     */
    void revokeOne(long sessionId);

    /**
     * Revokes all of the authenticated user's sessions except the one making this call.
     * {@code currentRefreshTokenValue} is the raw value of the {@code refreshToken} HttpOnly cookie
     * on the current request, used to identify (and exclude) the caller's own session.
     */
    void revokeAllOthers(String currentRefreshTokenValue);
}
