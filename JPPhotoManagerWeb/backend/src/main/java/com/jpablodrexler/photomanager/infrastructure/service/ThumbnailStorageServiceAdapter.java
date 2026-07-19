package com.jpablodrexler.photomanager.infrastructure.service;

import com.jpablodrexler.photomanager.domain.port.out.ThumbnailPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

/**
 * Disk-backed {@link ThumbnailPort} adapter, fronted by a Redis L2 cache (the
 * {@code redis-thumbnail-cache} feature). Thumbnail bytes are cached at key
 * {@code asset:thumbnail:{assetId}} with a configurable TTL so repeat reads across
 * load-balanced instances avoid disk I/O. Every Redis operation is best-effort: a
 * {@link DataAccessException} is caught, logged at {@code WARN}, and the method falls through
 * to disk-only behavior, mirroring {@code RedisRefreshTokenStore}'s fail-open pattern. Redis
 * caching can be disabled entirely via {@code photomanager.thumbnail-cache.enabled=false}.
 */
@Service
@Slf4j
public class ThumbnailStorageServiceAdapter implements ThumbnailPort {

    private static final String CACHE_KEY_PREFIX = "asset:thumbnail:";
    private static final String BLOB_SUFFIX = ".bin";

    @Value("${photomanager.thumbnails-directory:${user.home}/.photomanager/thumbnails}")
    private String thumbnailsDirectory;

    @Value("${photomanager.thumbnail-cache.enabled:true}")
    private boolean thumbnailCacheEnabled;

    @Value("${photomanager.thumbnail-cache.ttl-seconds:86400}")
    private long thumbnailCacheTtlSeconds;

    private final RedisTemplate<String, byte[]> thumbnailRedisTemplate;

    public ThumbnailStorageServiceAdapter(@Qualifier("thumbnailRedisTemplate") RedisTemplate<String, byte[]> thumbnailRedisTemplate) {
        this.thumbnailRedisTemplate = thumbnailRedisTemplate;
    }

    @Override
    public void saveThumbnail(String blobName, byte[] data) {
        boolean writtenToDisk;
        try {
            Path dir = Paths.get(thumbnailsDirectory);
            Files.createDirectories(dir);
            Files.write(dir.resolve(blobName), data);
            writtenToDisk = true;
        } catch (IOException e) {
            log.error("Failed to save thumbnail {}", blobName, e);
            writtenToDisk = false;
        }
        // Only cache bytes that are actually backed by a disk file — caching on a failed write
        // would create a Redis-only entry with nothing on disk to fall back to once it expires.
        if (thumbnailCacheEnabled && writtenToDisk) {
            putInCache(blobName, data);
        }
    }

    @Override
    public byte[] loadThumbnail(String blobName) {
        if (thumbnailCacheEnabled) {
            byte[] cached = getFromCache(blobName);
            if (cached != null) {
                return cached;
            }
        }

        byte[] diskBytes = loadFromDisk(blobName);
        if (thumbnailCacheEnabled && diskBytes != null) {
            putInCache(blobName, diskBytes);
        }
        return diskBytes;
    }

    @Override
    public void deleteThumbnail(String blobName) {
        try {
            Files.deleteIfExists(Paths.get(thumbnailsDirectory).resolve(blobName));
        } catch (IOException e) {
            log.error("Failed to delete thumbnail {}", blobName, e);
        }
        if (thumbnailCacheEnabled) {
            evictFromCache(blobName);
        }
    }

    @Override
    public boolean thumbnailExists(String blobName) {
        return Files.exists(Paths.get(thumbnailsDirectory).resolve(blobName));
    }

    private byte[] loadFromDisk(String blobName) {
        try {
            Path path = Paths.get(thumbnailsDirectory).resolve(blobName);
            if (Files.exists(path)) {
                return Files.readAllBytes(path);
            }
        } catch (IOException e) {
            log.error("Failed to load thumbnail {}", blobName, e);
        }
        return null;
    }

    private byte[] getFromCache(String blobName) {
        try {
            return thumbnailRedisTemplate.opsForValue().get(cacheKey(blobName));
        } catch (DataAccessException e) {
            log.warn("Redis read failed for thumbnail cache {}: {}", blobName, e.getMessage());
            return null;
        }
    }

    private void putInCache(String blobName, byte[] data) {
        try {
            thumbnailRedisTemplate.opsForValue().set(cacheKey(blobName), data, Duration.ofSeconds(thumbnailCacheTtlSeconds));
        } catch (DataAccessException e) {
            log.warn("Redis write failed for thumbnail cache {}: {}", blobName, e.getMessage());
        }
    }

    private void evictFromCache(String blobName) {
        try {
            thumbnailRedisTemplate.delete(cacheKey(blobName));
        } catch (DataAccessException e) {
            log.warn("Redis delete failed for thumbnail cache {}: {}", blobName, e.getMessage());
        }
    }

    private String cacheKey(String blobName) {
        String assetId = blobName.endsWith(BLOB_SUFFIX)
                ? blobName.substring(0, blobName.length() - BLOB_SUFFIX.length())
                : blobName;
        return CACHE_KEY_PREFIX + assetId;
    }
}
