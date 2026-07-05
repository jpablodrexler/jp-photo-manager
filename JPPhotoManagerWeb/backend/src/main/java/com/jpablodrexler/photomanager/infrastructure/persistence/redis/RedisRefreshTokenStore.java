package com.jpablodrexler.photomanager.infrastructure.persistence.redis;

import com.jpablodrexler.photomanager.domain.model.RefreshToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mirrors refresh-token writes into Redis alongside the existing PostgreSQL persistence.
 *
 * <p>Phase 1 of the {@code redis-refresh-tokens} migration: PostgreSQL remains the sole read
 * source of truth. This class only performs best-effort writes/deletes into Redis so a future
 * cutover to Redis-only reads/writes can reuse the same key scheme without redesign.
 *
 * <p>Key scheme:
 * <ul>
 *   <li>{@code refresh_token:{token}} — hash with {@code userId}, {@code tokenId}, {@code issuedAt}</li>
 *   <li>{@code refresh_tokens:user:{userId}} — set of token values belonging to a user</li>
 *   <li>{@code refresh_token:id:{tokenId}} — string mapping a Redis-native token id back to the token value</li>
 *   <li>{@code refresh_tokens:token_id_seq} — INCR counter generating Redis-native token ids</li>
 * </ul>
 *
 * <p>Every public method swallows {@link DataAccessException} (Spring Data Redis's exception
 * hierarchy, covering connection failures) and logs a warning instead of propagating — a Redis
 * outage must never fail a login or refresh request.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RedisRefreshTokenStore {

    private static final String TOKEN_KEY_PREFIX = "refresh_token:";
    private static final String USER_TOKENS_KEY_PREFIX = "refresh_tokens:user:";
    private static final String TOKEN_ID_KEY_PREFIX = "refresh_token:id:";
    private static final String TOKEN_ID_SEQ_KEY = "refresh_tokens:token_id_seq";
    private static final Duration MIN_TTL = Duration.ofSeconds(1);

    private final StringRedisTemplate redisTemplate;

    /**
     * Mirrors a newly issued (or rotated-in) token into Redis: a hash keyed by the token value,
     * a Redis-native {@code tokenId} (independent of the PostgreSQL {@code BIGSERIAL} id) obtained
     * via an atomic {@code INCR}, an id-index entry, and membership in the user's token set.
     */
    public void mirrorSave(RefreshToken token) {
        try {
            Duration ttl = computeTtl(token.getExpiresAt());
            Long redisTokenId = redisTemplate.opsForValue().increment(TOKEN_ID_SEQ_KEY);
            UUID userId = token.getUser().getId();

            String tokenKey = TOKEN_KEY_PREFIX + token.getToken();
            Map<String, String> fields = new HashMap<>();
            fields.put("userId", userId.toString());
            fields.put("tokenId", String.valueOf(redisTokenId));
            fields.put("issuedAt", token.getIssuedAt().toString());

            redisTemplate.opsForHash().putAll(tokenKey, fields);
            redisTemplate.expire(tokenKey, ttl);

            String idKey = TOKEN_ID_KEY_PREFIX + redisTokenId;
            redisTemplate.opsForValue().set(idKey, token.getToken(), ttl);

            redisTemplate.opsForSet().add(USER_TOKENS_KEY_PREFIX + userId, token.getToken());
        } catch (DataAccessException e) {
            log.warn("Redis mirror write failed for refresh token: {}", e.getMessage());
        }
    }

    /**
     * Deletes a revoked (or rotated-away) token's Redis mirror immediately: the token hash, its
     * id-index entry (if the Redis-native {@code tokenId} is known), and its membership in the
     * user's token set.
     */
    public void mirrorRevoke(String token, UUID userId, Long tokenId) {
        try {
            redisTemplate.delete(TOKEN_KEY_PREFIX + token);
            if (tokenId != null) {
                redisTemplate.delete(TOKEN_ID_KEY_PREFIX + tokenId);
            }
            if (userId != null) {
                redisTemplate.opsForSet().remove(USER_TOKENS_KEY_PREFIX + userId, token);
            }
        } catch (DataAccessException e) {
            log.warn("Redis mirror write failed for refresh token: {}", e.getMessage());
        }
    }

    private Duration computeTtl(Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        return ttl.compareTo(MIN_TTL) < 0 ? MIN_TTL : ttl;
    }
}
