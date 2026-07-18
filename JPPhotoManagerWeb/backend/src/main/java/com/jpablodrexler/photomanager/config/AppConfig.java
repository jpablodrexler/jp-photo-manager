package com.jpablodrexler.photomanager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableScheduling
@EnableCaching
public class AppConfig {

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

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.registerCustomCache("home-stats",
                Caffeine.newBuilder()
                        .maximumSize(1)
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .build());
        cacheManager.registerCustomCache("sub-folders",
                Caffeine.newBuilder()
                        .maximumSize(500)
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .build());
        cacheManager.registerCustomCache("asset-exif",
                Caffeine.newBuilder()
                        .maximumSize(2000)
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .build());
        return cacheManager;
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
