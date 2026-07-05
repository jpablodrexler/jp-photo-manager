package com.jpablodrexler.photomanager.infrastructure.persistence.adapter;

import com.jpablodrexler.photomanager.domain.model.RefreshToken;
import com.jpablodrexler.photomanager.domain.port.out.RefreshTokenRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.jpa.JpaRefreshTokenRepository;
import com.jpablodrexler.photomanager.infrastructure.persistence.mapper.RefreshTokenEntityMapper;
import com.jpablodrexler.photomanager.infrastructure.persistence.redis.RedisRefreshTokenStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final JpaRefreshTokenRepository jpa;
    private final RefreshTokenEntityMapper mapper;
    private final RedisRefreshTokenStore redisRefreshTokenStore;

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        return jpa.findByToken(token).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public RefreshToken save(RefreshToken token) {
        RefreshToken saved = mapper.toDomain(jpa.save(mapper.toEntity(token)));
        mirrorToRedis(saved);
        return saved;
    }

    /**
     * Mirrors the just-saved token into Redis: a revoked token (the pattern used by
     * {@code validateAndRotate} to invalidate the token being rotated away from) is deleted from
     * Redis immediately rather than mirrored with a {@code revoked} flag; any other save is
     * mirrored as a fresh/updated hash. Defensively swallows any exception so a Redis-side failure
     * (including a bug in {@link RedisRefreshTokenStore} itself) never fails the PostgreSQL write
     * that already completed above.
     */
    private void mirrorToRedis(RefreshToken saved) {
        try {
            if (saved.isRevoked()) {
                redisRefreshTokenStore.mirrorRevoke(saved.getToken(), saved.getUser().getId(), saved.getTokenId());
            } else {
                redisRefreshTokenStore.mirrorSave(saved);
            }
        } catch (Exception e) {
            log.warn("Redis mirror write failed for refresh token: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteByUserId(UUID userId) {
        jpa.deleteByUser_Id(userId);
    }

    @Override
    @Transactional
    public void deleteById(Long tokenId) {
        jpa.deleteById(tokenId);
    }
}
