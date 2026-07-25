package com.jpablodrexler.photomanager.domain.port.in.auth;

import com.jpablodrexler.photomanager.domain.model.SessionInfo;

import java.util.List;

public interface GetActiveSessionsUseCase {

    /**
     * Lists the authenticated user's non-expired refresh-token sessions, ordered most-recently-used
     * first. {@code currentRefreshTokenValue} is the raw value of the {@code refreshToken} HttpOnly
     * cookie on the current request (may be {@code null}), used only to flag which returned session
     * is the one making this call.
     */
    List<SessionInfo> execute(String currentRefreshTokenValue);
}
