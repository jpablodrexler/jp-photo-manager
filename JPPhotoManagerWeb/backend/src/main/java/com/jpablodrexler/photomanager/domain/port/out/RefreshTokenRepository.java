package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.domain.model.RefreshToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findById(Long tokenId);

    /**
     * Returns only the user's non-revoked, non-expired tokens as of {@code now}, filtered at the
     * database level. {@code validateAndRotate} marks a token revoked (rather than deleting it) on
     * every refresh, so a user's full token history can grow without bound over time; fetching and
     * filtering that whole history in memory would get progressively more expensive per call.
     */
    List<RefreshToken> findActiveByUserId(UUID userId, Instant now);

    RefreshToken save(RefreshToken token);

    void deleteByUserId(UUID userId);

    void deleteById(Long tokenId);
}
