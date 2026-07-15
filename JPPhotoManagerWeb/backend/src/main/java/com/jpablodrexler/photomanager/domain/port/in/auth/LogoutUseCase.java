package com.jpablodrexler.photomanager.domain.port.in.auth;

public interface LogoutUseCase {

    /**
     * Best-effort logout: extracts the username from {@code jwtToken} (if present and valid) and
     * revokes all of that user's refresh tokens. Never throws — an invalid/missing token or a
     * revocation failure is treated as a no-op, since clearing the client's cookies is what
     * actually ends the session.
     */
    void execute(String jwtToken);
}
