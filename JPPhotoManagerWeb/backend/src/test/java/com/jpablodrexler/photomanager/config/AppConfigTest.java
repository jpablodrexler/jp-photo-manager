package com.jpablodrexler.photomanager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.infrastructure.web.filter.RequestCorrelationFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.core.Ordered;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AppConfigTest {

    @Mock RedisConnectionFactory redisConnectionFactory;

    AppConfig sut = new AppConfig();

    @Test
    void taskExecutor_wrapsDelegatingSecurityContextExecutorAndRunsSubmittedTasks() throws Exception {
        AsyncTaskExecutor executor = sut.taskExecutor();

        assertThat(executor).isInstanceOf(DelegatingSecurityContextAsyncTaskExecutor.class);

        CompletableFuture<String> ran = new CompletableFuture<>();
        executor.execute(() -> ran.complete("done"));

        assertThat(ran.get(5, TimeUnit.SECONDS)).isEqualTo("done");
    }

    @Test
    void catalogTaskScheduler_configuredWithExpectedThreadNamePrefix() {
        ThreadPoolTaskScheduler scheduler = sut.catalogTaskScheduler();

        assertThat(scheduler.getThreadNamePrefix()).isEqualTo("catalog-scheduler-");
        // getPoolSize() reports live thread count, which stays 0 until a task is actually
        // scheduled; the configured core pool size is only visible via the underlying executor.
        assertThat(scheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);
    }

    @Test
    void cacheManager_returnsRedisCacheManagerServingConfiguredAndFallbackCaches() {
        CacheManager cacheManager = sut.cacheManager(redisConnectionFactory, new ObjectMapper());

        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
        assertThat(cacheManager.getCache("home-stats")).isNotNull();
        assertThat(cacheManager.getCache("sub-folders")).isNotNull();
        assertThat(cacheManager.getCache("asset-exif")).isNotNull();
        assertThat(cacheManager.getCache("assets")).isNotNull();
        assertThat(cacheManager.getCache("tags")).isNotNull();
        // Any other cache name falls back to the generic GenericJackson2JsonRedisSerializer config.
        assertThat(cacheManager.getCache("some-other-cache")).isNotNull();
    }

    @Test
    void errorHandler_delegatesToCacheErrorHandlerBean() {
        CacheErrorHandler errorHandler = sut.errorHandler();

        assertThat(errorHandler).isInstanceOf(LoggingCacheErrorHandler.class);
        assertThat(sut.cacheErrorHandler()).isInstanceOf(LoggingCacheErrorHandler.class);
    }

    @Test
    void thumbnailRedisTemplate_configuresStringKeyAndByteArrayValueSerializers() {
        RedisTemplate<String, byte[]> template = sut.thumbnailRedisTemplate(redisConnectionFactory);

        assertThat(template.getConnectionFactory()).isSameAs(redisConnectionFactory);
        assertThat(template.getKeySerializer()).isInstanceOf(StringRedisSerializer.class);

        byte[] payload = {1, 2, 3};
        org.springframework.data.redis.serializer.RedisSerializer<Object> valueSerializer =
                (org.springframework.data.redis.serializer.RedisSerializer<Object>) template.getValueSerializer();
        byte[] roundTripped = (byte[]) valueSerializer.deserialize(valueSerializer.serialize(payload));
        assertThat(roundTripped).isEqualTo(payload);
    }

    @Test
    void corsFilter_appliesConfiguredOriginsAndMethodsToApiPaths() {
        ReflectionTestUtils.setField(sut, "corsAllowedOrigins", List.of("http://localhost:4200"));

        CorsFilter corsFilter = sut.corsFilter();

        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) ReflectionTestUtils.getField(corsFilter, "configSource");
        CorsConfiguration config = source.getCorsConfigurations().get("/api/**");

        assertThat(config.getAllowedOrigins()).containsExactly("http://localhost:4200");
        assertThat(config.getAllowedMethods()).contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getExposedHeaders()).containsExactly("X-Request-ID");
    }

    @Test
    void requestCorrelationFilter_registeredAtHighestPrecedence() {
        FilterRegistrationBean<RequestCorrelationFilter> registration = sut.requestCorrelationFilter();

        assertThat(registration.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
        assertThat(registration.getFilter()).isInstanceOf(RequestCorrelationFilter.class);
    }
}
