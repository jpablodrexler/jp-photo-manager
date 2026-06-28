package com.jpablodrexler.photomanager.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;
import java.util.concurrent.Executor;

@Configuration
@EnableScheduling
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
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
            LettuceBasedProxyManager<String> rateLimitProxyManager, ObjectMapper objectMapper) {
        FilterRegistrationBean<RateLimitFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new RateLimitFilter(rateLimitProxyManager, objectMapper, trustedProxyIpsRaw));
        reg.addUrlPatterns("/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        return reg;
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
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("photomanager-async-");
        executor.initialize();
        return executor;
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
