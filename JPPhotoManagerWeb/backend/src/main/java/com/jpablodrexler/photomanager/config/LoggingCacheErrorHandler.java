package com.jpablodrexler.photomanager.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;

/**
 * Fail-open {@link org.springframework.cache.interceptor.CacheErrorHandler}: logs Redis cache
 * get/put/evict/clear failures at {@code WARN} instead of letting {@link SimpleCacheErrorHandler}'s
 * default behavior propagate the exception, which would otherwise turn a Redis outage into a
 * request failure for every {@code @Cacheable}/{@code @CacheEvict}-annotated use case. Mirrors the
 * fail-open convention already established by {@code redis-thumbnail-cache} and
 * {@code redis-refresh-tokens}.
 */
@Slf4j
public class LoggingCacheErrorHandler extends SimpleCacheErrorHandler {

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache GET failed for cache={} key={}: {}", cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Cache PUT failed for cache={} key={}: {}", cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Cache EVICT failed for cache={} key={}: {}", cache.getName(), key, exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Cache CLEAR failed for cache={}: {}", cache.getName(), exception.getMessage());
    }
}
