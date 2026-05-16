package com.jpablodrexler.photomanager.domain.port.out;

import com.jpablodrexler.photomanager.domain.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findByToken(String token);

    RefreshToken save(RefreshToken token);

    void deleteByUserId(UUID userId);

    void deleteById(Long tokenId);
}
