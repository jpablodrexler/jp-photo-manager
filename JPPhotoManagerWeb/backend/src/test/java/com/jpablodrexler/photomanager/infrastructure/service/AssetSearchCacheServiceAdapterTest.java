package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.port.out.AssetSearchCachePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the cursor-based {@code SCAN}/{@code DEL} eviction behind {@link AssetSearchCachePort},
 * shared by {@code AssetSearchCacheInvalidationListener} (catalog/delete Kafka events) and the tag
 * mutation use cases (see {@code redis-search-tag-cache}).
 */
@ExtendWith(MockitoExtension.class)
class AssetSearchCacheServiceAdapterTest {

    @Mock RedisConnectionFactory redisConnectionFactory;
    @Mock RedisConnection redisConnection;
    @Mock Cursor<byte[]> cursor;

    @InjectMocks
    AssetSearchCacheServiceAdapter sut;

    @Test
    void evictFolder_nullFolderId_doesNotThrowAndSkipsRedis() {
        assertThatCode(() -> sut.evictFolder(null)).doesNotThrowAnyException();
    }

    @Test
    void evictFolder_multipleMatchingKeys_deletesAllOfThem() {
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn("assets:5:a".getBytes(), "assets:5:b".getBytes());

        sut.evictFolder(5L);

        verify(redisConnection).del(new byte[][]{"assets:5:a".getBytes(), "assets:5:b".getBytes()});
    }

    @Test
    void evictFolder_noMatchingKeys_doesNotCallDelete() {
        when(redisConnectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(false);

        sut.evictFolder(5L);

        verify(redisConnection, never()).del(any(byte[][].class));
    }

    @Test
    void evictFolder_redisThrows_isCaughtAndLogged() {
        when(redisConnectionFactory.getConnection()).thenThrow(new RedisConnectionFailureException("down"));

        assertThatCode(() -> sut.evictFolder(5L)).doesNotThrowAnyException();
    }
}
