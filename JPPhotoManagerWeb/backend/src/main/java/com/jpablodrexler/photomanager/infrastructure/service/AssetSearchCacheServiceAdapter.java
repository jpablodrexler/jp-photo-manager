package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.port.out.AssetSearchCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Evicts every {@code assets} cache entry belonging to a folder ({@code assets:{folderId}:*}) using
 * a cursor-based {@code SCAN} (never a blocking {@code KEYS}), so a shared production Redis instance
 * also serving the rate limiter and refresh-token store never stalls on eviction. Shared by
 * {@link com.jpablodrexler.photomanager.infrastructure.kafka.AssetSearchCacheInvalidationListener}
 * (catalog/delete events) and the tag mutation use cases (add/remove/bulk-add/bulk-remove), since a
 * tag change can alter a folder's tag-filtered search results just as much as a catalog or delete
 * can.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AssetSearchCacheServiceAdapter implements AssetSearchCachePort {

    private static final int SCAN_BATCH_SIZE = 200;

    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public void evictFolder(Long folderId) {
        if (folderId == null) {
            log.debug("Skipping assets cache eviction: folderId is null");
            return;
        }

        try {
            String pattern = "assets:" + folderId + ":*";
            List<byte[]> keysToDelete = new ArrayList<>();
            try (RedisConnection connection = redisConnectionFactory.getConnection()) {
                try (Cursor<byte[]> cursor = connection.scan(ScanOptions.scanOptions()
                        .match(pattern)
                        .count(SCAN_BATCH_SIZE)
                        .build())) {
                    while (cursor.hasNext()) {
                        keysToDelete.add(cursor.next());
                    }
                }
                if (!keysToDelete.isEmpty()) {
                    connection.del(keysToDelete.toArray(new byte[0][]));
                }
            }
            log.debug("Evicted {} assets cache entries for folderId={}", keysToDelete.size(), folderId);
        } catch (Exception e) {
            log.warn("Failed to evict assets cache entries for folderId={}: {}", folderId, e.getMessage());
        }
    }
}
