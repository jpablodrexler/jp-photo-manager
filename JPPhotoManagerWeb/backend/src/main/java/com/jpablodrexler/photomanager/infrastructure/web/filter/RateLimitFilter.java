package com.jpablodrexler.photomanager.infrastructure.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jpablodrexler.photomanager.domain.port.out.JwtTokenPort;
import com.jpablodrexler.photomanager.infrastructure.web.dto.request.AuthRequestDto;
import com.jpablodrexler.photomanager.infrastructure.web.dto.response.ErrorResponseDto;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class RateLimitFilter implements Filter {

    private static final String LOGIN_ENDPOINT   = "/api/auth/login";
    private static final String CATALOG_ENDPOINT = "/api/assets/catalog";
    private static final String JWT_COOKIE_NAME  = "jwt";

    private final ProxyManager<String> rateLimitProxyManager;
    private final ObjectMapper objectMapper;
    private final JwtTokenPort jwtTokenPort;
    private final Set<String> trustedProxyIps;
    private final Set<String> rateLimitExemptUsernames;

    public RateLimitFilter(ProxyManager<String> rateLimitProxyManager, ObjectMapper objectMapper,
                           JwtTokenPort jwtTokenPort, String trustedProxyIpsRaw,
                           String rateLimitExemptUsernamesRaw) {
        this.rateLimitProxyManager = rateLimitProxyManager;
        this.objectMapper = objectMapper;
        this.jwtTokenPort = jwtTokenPort;
        this.trustedProxyIps = parseCommaSeparated(trustedProxyIpsRaw);
        this.rateLimitExemptUsernames = parseCommaSeparated(rateLimitExemptUsernamesRaw);
    }

    private static Set<String> parseCommaSeparated(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        return Arrays.stream(raw.split(","))
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

        if (!rateLimitExemptUsernames.isEmpty()) {
            if ("catalog".equals(endpointKey) && isExemptViaJwtCookie(req)) {
                chain.doFilter(request, response);
                return;
            }
            if ("login".equals(endpointKey)) {
                HttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(req);
                if (isExemptViaLoginBody((CachedBodyHttpServletRequest) cachedRequest)) {
                    chain.doFilter(cachedRequest, response);
                    return;
                }
                // The body was already consumed to check for exemption above; re-dispatch the
                // cached-body wrapper so AuthController can still read it normally.
                req = cachedRequest;
                request = cachedRequest;
            }
        }

        String clientIp  = resolveClientIp(req);
        String bucketKey = clientIp + ":" + endpointKey;

        Bucket bucket;
        try {
            bucket = rateLimitProxyManager.builder()
                .build(bucketKey, () -> BucketConfiguration.builder()
                    .addLimit(createLimit(endpointKey))
                    .build());
        } catch (Exception e) {
            log.warn("Redis unavailable for rate limiting, allowing request through: {}", e.getMessage());
            chain.doFilter(request, response);
            return;
        }

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

        ErrorResponseDto body = new ErrorResponseDto(
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

    private Bandwidth createLimit(String endpointKey) {
        return switch (endpointKey) {
            case "login"   -> Bandwidth.classic(10, Refill.intervally(10, Duration.ofSeconds(60)));
            case "catalog" -> Bandwidth.classic(5,  Refill.intervally(5,  Duration.ofSeconds(3600)));
            default        -> throw new IllegalArgumentException("Unknown endpoint key: " + endpointKey);
        };
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

    // Runs before JwtAuthenticationFilter in the chain (see SecurityConfig), so
    // SecurityContextHolder isn't populated yet here -- the "jwt" cookie is read and validated
    // directly, the same way JwtAuthenticationFilter.resolveToken does, purely to decide whether
    // this specific request is exempt from the catalog rate limit.
    private boolean isExemptViaJwtCookie(HttpServletRequest req) {
        if (req.getCookies() == null) return false;
        return Arrays.stream(req.getCookies())
                .filter(c -> JWT_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(jwtTokenPort::isTokenValid)
                .map(jwtTokenPort::extractUsername)
                .anyMatch(rateLimitExemptUsernames::contains);
    }

    // The login endpoint has no JWT yet (that's the point of it), so the only signal available
    // pre-authentication is the username submitted in the request body. This intentionally trades
    // away brute-force protection for whichever usernames are configured as exempt -- see
    // photomanager.rate-limit-exempt-usernames's documentation in application.yml, which is meant
    // only for a dedicated, throwaway test account, never a real user account.
    private boolean isExemptViaLoginBody(CachedBodyHttpServletRequest cachedRequest) {
        try {
            AuthRequestDto body = objectMapper.readValue(cachedRequest.getContentAsByteArray(), AuthRequestDto.class);
            return body.username() != null && rateLimitExemptUsernames.contains(body.username());
        } catch (IOException e) {
            return false;
        }
    }

    // Wraps the request so its body can be read here (to check for a login exemption) and then
    // read again, unconsumed, by AuthController's @RequestBody binding -- a plain
    // HttpServletRequest's input stream can only be consumed once.
    private static final class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
        private final byte[] cachedBody;

        CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = request.getInputStream().readAllBytes();
        }

        byte[] getContentAsByteArray() {
            return cachedBody;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int read() {
                    return byteArrayInputStream.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
        }
    }
}
