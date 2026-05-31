package com.jpablodrexler.photomanager.infrastructure.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.infrastructure.web.ErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter implements Filter {

    private static final String LOGIN_ENDPOINT   = "/api/auth/login";
    private static final String CATALOG_ENDPOINT = "/api/assets/catalog";

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Value("${photomanager.trusted-proxy-ips:}")
    private String trustedProxyIpsRaw;

    private Set<String> trustedProxyIps;

    @PostConstruct
    public void init() {
        if (trustedProxyIpsRaw == null || trustedProxyIpsRaw.isBlank()) {
            trustedProxyIps = Set.of();
            return;
        }
        trustedProxyIps = Arrays.stream(trustedProxyIpsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String uri    = req.getRequestURI();
        String method = req.getMethod();

        String endpointKey = resolveEndpointKey(method, uri);
        if (endpointKey == null) {
            chain.doFilter(request, response);
            return;
        }

        String  clientIp  = resolveClientIp(req);
        String  bucketKey = clientIp + ":" + endpointKey;
        Bucket  bucket    = buckets.computeIfAbsent(bucketKey, k -> createBucket(endpointKey));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            chain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;
        log.warn("Rate limit exceeded for IP={} endpoint={}", clientIp, endpointKey);

        resp.setStatus(429);
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));

        ErrorResponse body = new ErrorResponse(
                Instant.now().toString(),
                429,
                "Too Many Requests",
                "Too many requests. Please try again later.");
        resp.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private String resolveEndpointKey(String method, String uri) {
        if ("POST".equalsIgnoreCase(method) && LOGIN_ENDPOINT.equals(uri)) {
            return "login";
        }
        if ("GET".equalsIgnoreCase(method) && CATALOG_ENDPOINT.equals(uri)) {
            return "catalog";
        }
        return null;
    }

    private Bucket createBucket(String endpointKey) {
        Bandwidth limit = switch (endpointKey) {
            case "login"   -> Bandwidth.classic(10, Refill.intervally(10, Duration.ofSeconds(60)));
            case "catalog" -> Bandwidth.classic(5,  Refill.intervally(5,  Duration.ofSeconds(3600)));
            default        -> throw new IllegalArgumentException("Unknown endpoint key: " + endpointKey);
        };
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientIp(HttpServletRequest req) {
        String remoteAddr = req.getRemoteAddr();
        if (trustedProxyIps.contains(remoteAddr)) {
            String forwarded = req.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }
}
