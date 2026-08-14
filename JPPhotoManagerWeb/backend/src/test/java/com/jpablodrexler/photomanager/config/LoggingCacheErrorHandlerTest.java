package com.jpablodrexler.photomanager.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.cache.Cache;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Verifies the fail-open contract: a Redis cache failure is logged, never rethrown, so a
 * {@code @Cacheable}/{@code @CacheEvict}-annotated use case falls back to its normal data source
 * instead of turning a cache hiccup into a request failure.
 */
@ExtendWith(MockitoExtension.class)
class LoggingCacheErrorHandlerTest {

    @Mock Cache cache;

    LoggingCacheErrorHandler sut = new LoggingCacheErrorHandler();

    @Test
    void handleCacheGetError_doesNotPropagateException() {
        assertThatCode(() -> sut.handleCacheGetError(new RuntimeException("redis down"), cache, "key"))
                .doesNotThrowAnyException();
    }

    @Test
    void handleCachePutError_doesNotPropagateException() {
        assertThatCode(() -> sut.handleCachePutError(new RuntimeException("redis down"), cache, "key", "value"))
                .doesNotThrowAnyException();
    }

    @Test
    void handleCacheEvictError_doesNotPropagateException() {
        assertThatCode(() -> sut.handleCacheEvictError(new RuntimeException("redis down"), cache, "key"))
                .doesNotThrowAnyException();
    }

    @Test
    void handleCacheClearError_doesNotPropagateException() {
        assertThatCode(() -> sut.handleCacheClearError(new RuntimeException("redis down"), cache))
                .doesNotThrowAnyException();
    }
}
