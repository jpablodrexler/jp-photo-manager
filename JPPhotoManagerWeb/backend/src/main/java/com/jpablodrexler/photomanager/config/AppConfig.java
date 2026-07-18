package com.jpablodrexler.photomanager.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.model.AssetExif;
import com.jpablodrexler.photomanager.domain.model.Folder;
import com.jpablodrexler.photomanager.domain.model.HomeStats;
import com.jpablodrexler.photomanager.domain.model.PaginatedResult;
import com.jpablodrexler.photomanager.domain.model.Tag;
import com.jpablodrexler.photomanager.infrastructure.web.filter.RateLimitFilter;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.github.mweirauch.micrometer.jvm.extras.ProcessMemoryMetrics;
import io.github.mweirauch.micrometer.jvm.extras.ProcessThreadMetrics;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Configuration
@EnableScheduling
@EnableCaching
public class AppConfig implements CachingConfigurer {

    @Value("${photomanager.cors-allowed-origins}")
    private List<String> corsAllowedOrigins;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${photomanager.trusted-proxy-ips:}")
    private String trustedProxyIpsRaw;

    @Bean(destroyMethod = "shutdown")
    public RedisClient rateLimitRedisClient() {
        return RedisClient.create(RedisURI.builder().withHost(redisHost).withPort(redisPort).build());
    }

    @Bean
    public LettuceBasedProxyManager<String> rateLimitProxyManager(RedisClient rateLimitRedisClient) {
        StatefulRedisConnection<String, byte[]> connection =
            rateLimitRedisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));
        return LettuceBasedProxyManager.builderFor(connection).build();
    }

    @Bean
    public RateLimitFilter rateLimitFilter(LettuceBasedProxyManager<String> rateLimitProxyManager,
                                           ObjectMapper objectMapper) {
        return new RateLimitFilter(rateLimitProxyManager, objectMapper, trustedProxyIpsRaw);
    }

    @Bean
    public ProcessMemoryMetrics processMemoryMetrics() {
        return new ProcessMemoryMetrics();
    }

    @Bean
    public ProcessThreadMetrics processThreadMetrics() {
        return new ProcessThreadMetrics();
    }

    @Bean(name = "taskExecutor")
    public AsyncTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("photomanager-async-");
        executor.initialize();
        // @Async("taskExecutor") methods combined with @PreAuthorize (ConvertAssetsUseCaseImpl,
        // SyncAssetsUseCaseImpl) run their security check on this pool's worker threads, which do
        // not inherit the calling thread's SecurityContext by default — every such call would
        // fail with AuthenticationCredentialsNotFoundException regardless of the caller's role.
        // Wrapping the executor propagates the SecurityContext present at submission time to the
        // worker thread for the task's duration. The bean is explicitly requested by name
        // ("taskExecutor") from those two @Async annotations rather than relying on @Async's
        // default-executor lookup, since Spring Boot may also auto-configure its own
        // "applicationTaskExecutor" TaskExecutor bean, making by-type resolution ambiguous.
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }

    @Bean(name = "catalogTaskScheduler")
    public ThreadPoolTaskScheduler catalogTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("catalog-scheduler-");
        scheduler.initialize();
        return scheduler;
    }

    /**
     * Backs all five named caches ({@code home-stats}, {@code sub-folders}, {@code asset-exif},
     * {@code assets}, {@code tags}) with Redis instead of a per-JVM Caffeine cache, so a write on
     * one backend instance correctly invalidates the copy served by another
     * (see {@code redis-search-tag-cache}). Reuses the auto-configured {@link RedisConnectionFactory}
     * already provisioned for the refresh-token store and thumbnail cache — no new connection
     * plumbing. Keys are prefixed {@code <cacheName>:} (single colon, via
     * {@code computePrefixWith}) rather than Spring's default {@code <cacheName>::} (double colon)
     * so the {@code assets} cache's keys can be pattern-matched as {@code assets:{folderId}:*} for
     * folder-scoped eviction.
     *
     * <p>Each cache gets its own {@link Jackson2JsonRedisSerializer} built for its exact declared
     * return type (via the application's auto-configured {@link ObjectMapper}, so {@code JavaTimeModule}
     * etc. still apply) rather than one shared {@link GenericJackson2JsonRedisSerializer} with
     * {@code @class} polymorphic type hints. Two problems ruled that generic approach out (both
     * confirmed via {@code AssetSearchTagCacheIntegrationTest} while developing
     * {@code redis-search-tag-cache}): (1) enabling default typing mutates the {@code ObjectMapper}
     * instance in place — even a {@link ObjectMapper#copy()} only defers the problem — and (2) Jackson
     * writes the type hint differently for a top-level JSON object ({@code {"@class":...}}) versus a
     * top-level JSON array ({@code ["java.util.ArrayList",[...]]}), so the {@code tags} cache (whose
     * value is a bare {@code List<Tag>}) silently deserialized into the wrong shape and the cache
     * effectively never hit. Serializing against an explicit {@link JavaType} per cache sidesteps type
     * hints entirely — there is only one possible shape to read back.
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {
        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(name -> name + ":");

        Map<String, RedisCacheConfiguration> perCacheConfigurations = Map.of(
                "home-stats", typedConfig(baseConfig, objectMapper, HomeStats.class)
                        .entryTtl(Duration.ofMinutes(10)),
                "sub-folders", typedConfig(baseConfig, objectMapper,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Folder.class))
                        .entryTtl(Duration.ofMinutes(5)),
                "asset-exif", typedConfig(baseConfig, objectMapper, AssetExif.class)
                        .entryTtl(Duration.ofMinutes(30)),
                "assets", typedConfig(baseConfig, objectMapper,
                        objectMapper.getTypeFactory().constructParametricType(PaginatedResult.class, Asset.class))
                        .entryTtl(Duration.ofMinutes(5)),
                "tags", typedConfig(baseConfig, objectMapper,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Tag.class))
                        .entryTtl(Duration.ofMinutes(5))
        );

        RedisCacheConfiguration fallbackConfig = baseConfig.serializeValuesWith(RedisSerializationContext
                .SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(fallbackConfig)
                .withInitialCacheConfigurations(perCacheConfigurations)
                .build();
    }

    private RedisCacheConfiguration typedConfig(RedisCacheConfiguration base, ObjectMapper objectMapper, Class<?> type) {
        return typedConfig(base, objectMapper, objectMapper.getTypeFactory().constructType(type));
    }

    private RedisCacheConfiguration typedConfig(RedisCacheConfiguration base, ObjectMapper objectMapper, JavaType type) {
        return base.serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, type)));
    }

    /**
     * Extends the fail-open convention already applied to {@code redis-thumbnail-cache} and
     * {@code redis-refresh-tokens} to the Spring Cache abstraction: a Redis outage during a
     * cache get/put/evict is caught and logged at {@code WARN} rather than propagated, so
     * {@code @Cacheable}/{@code @CacheEvict}-annotated use cases fall back to querying their
     * normal data source instead of turning a cache hiccup into a request failure.
     */
    @Override
    public CacheErrorHandler errorHandler() {
        return cacheErrorHandler();
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new LoggingCacheErrorHandler();
    }

    /**
     * Dedicated {@code RedisTemplate<String, byte[]>} bean for the {@code redis-thumbnail-cache}
     * L2 cache in {@code ThumbnailStorageServiceAdapter}. Explicitly named so it never collides
     * with Spring Boot's auto-configured default {@code RedisTemplate<Object, Object>} bean, and
     * uses raw byte serialization for values (no JDK object-serialization envelope) since thumbnail
     * bytes are already a serialized JPEG payload. Requires the Redis deployment to run with the
     * {@code allkeys-lru} eviction policy so cache growth is bounded automatically.
     */
    @Bean(name = "thumbnailRedisTemplate")
    public RedisTemplate<String, byte[]> thumbnailRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, byte[]> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(RedisSerializer.byteArray());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsAllowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return new CorsFilter(source);
    }
}
