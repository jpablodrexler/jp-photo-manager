package com.jpablodrexler.photomanager.infrastructure.persistence.redis;

import com.jpablodrexler.photomanager.domain.model.RefreshToken;
import com.jpablodrexler.photomanager.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRefreshTokenStoreTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock HashOperations<String, Object, Object> hashOperations;
    @Mock SetOperations<String, String> setOperations;
    @Mock ValueOperations<String, String> valueOperations;

    RedisRefreshTokenStore sut;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        sut = new RedisRefreshTokenStore(redisTemplate);
    }

    private RefreshToken newToken(UUID userId, Instant issuedAt, Instant expiresAt) {
        User user = User.builder().id(userId).username("alice").build();
        return RefreshToken.builder()
                .user(user)
                .token("tok-value")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();
    }

    @Test
    void mirrorSave_nonRevokedToken_writesHashWithCorrectFieldsAndTtl() {
        UUID userId = UUID.randomUUID();
        Instant issuedAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant expiresAt = issuedAt.plus(30, ChronoUnit.DAYS);
        RefreshToken token = newToken(userId, issuedAt, expiresAt);
        when(valueOperations.increment("refresh_tokens:token_id_seq")).thenReturn(42L);

        sut.mirrorSave(token);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(hashOperations).putAll(eq("refresh_token:tok-value"), captor.capture());
        Map<String, String> fields = captor.getValue();
        assertThat(fields).containsEntry("userId", userId.toString());
        assertThat(fields).containsEntry("tokenId", "42");
        assertThat(fields).containsEntry("issuedAt", issuedAt.toString());

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(redisTemplate).expire(eq("refresh_token:tok-value"), ttlCaptor.capture());
        assertThat(ttlCaptor.getValue()).isPositive();
    }

    @Test
    void mirrorSave_obtainsNewIdViaIncrAndWritesIdIndexKey() {
        UUID userId = UUID.randomUUID();
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(30, ChronoUnit.DAYS);
        RefreshToken token = newToken(userId, issuedAt, expiresAt);
        when(valueOperations.increment("refresh_tokens:token_id_seq")).thenReturn(7L);

        sut.mirrorSave(token);

        verify(valueOperations).increment("refresh_tokens:token_id_seq");
        verify(valueOperations).set(eq("refresh_token:id:7"), eq("tok-value"), any(Duration.class));
        verify(setOperations).add("refresh_tokens:user:" + userId, "tok-value");
    }

    @Test
    void mirrorSave_expiresAtInPast_clampsTtlToOneSecond() {
        UUID userId = UUID.randomUUID();
        Instant issuedAt = Instant.now().minus(31, ChronoUnit.DAYS);
        Instant expiresAt = Instant.now().minus(1, ChronoUnit.DAYS);
        RefreshToken token = newToken(userId, issuedAt, expiresAt);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        sut.mirrorSave(token);

        verify(redisTemplate).expire("refresh_token:tok-value", Duration.ofSeconds(1));
        verify(valueOperations).set(eq("refresh_token:id:1"), eq("tok-value"), eq(Duration.ofSeconds(1)));
    }

    @Test
    void mirrorSave_dataAccessExceptionThrown_swallowsExceptionWithoutThrowing() {
        when(valueOperations.increment(anyString()))
                .thenThrow(new RedisConnectionFailureException("Redis down"));
        RefreshToken token = newToken(UUID.randomUUID(), Instant.now(), Instant.now().plusSeconds(60));

        assertThatCode(() -> sut.mirrorSave(token)).doesNotThrowAnyException();
    }

    @Test
    void mirrorRevoke_deletesHashIdIndexAndSetMembership() {
        UUID userId = UUID.randomUUID();

        sut.mirrorRevoke("tok-value", userId, 42L);

        verify(redisTemplate).delete("refresh_token:tok-value");
        verify(redisTemplate).delete("refresh_token:id:42");
        verify(setOperations).remove("refresh_tokens:user:" + userId, "tok-value");
    }

    @Test
    void mirrorRevoke_nullTokenId_skipsIdIndexDeletion() {
        UUID userId = UUID.randomUUID();

        sut.mirrorRevoke("tok-value", userId, null);

        verify(redisTemplate).delete("refresh_token:tok-value");
        verify(redisTemplate, never()).delete("refresh_token:id:null");
        verify(setOperations).remove("refresh_tokens:user:" + userId, "tok-value");
    }

    @Test
    void mirrorRevoke_dataAccessExceptionThrown_swallowsExceptionWithoutThrowing() {
        doThrow(new RedisConnectionFailureException("Redis down"))
                .when(redisTemplate).delete("refresh_token:tok-value");

        assertThatCode(() -> sut.mirrorRevoke("tok-value", UUID.randomUUID(), 1L))
                .doesNotThrowAnyException();
    }
}
