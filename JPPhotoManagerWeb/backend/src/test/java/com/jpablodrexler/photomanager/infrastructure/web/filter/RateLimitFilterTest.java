package com.jpablodrexler.photomanager.infrastructure.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock FilterChain chain;

    RateLimitFilter sut;

    @BeforeEach
    void setUp() {
        sut = new RateLimitFilter(inMemoryProxyManager(), new ObjectMapper(), "");
    }

    @Test
    void loginEndpoint_11thRequestFromSameIp_returns429() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest  req  = loginRequest("10.0.0.1");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            sut.doFilter(req, resp, chain);
            assertThat(resp.getStatus()).isEqualTo(200);
        }

        MockHttpServletRequest  req11  = loginRequest("10.0.0.1");
        MockHttpServletResponse resp11 = new MockHttpServletResponse();
        sut.doFilter(req11, resp11, chain);

        assertThat(resp11.getStatus()).isEqualTo(429);
        verify(chain, times(10)).doFilter(any(), any());
    }

    @Test
    void catalogEndpoint_6thRequestFromSameIp_returns429() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest  req  = catalogRequest("10.0.0.2");
            MockHttpServletResponse resp = new MockHttpServletResponse();
            sut.doFilter(req, resp, chain);
            assertThat(resp.getStatus()).isEqualTo(200);
        }

        MockHttpServletRequest  req6  = catalogRequest("10.0.0.2");
        MockHttpServletResponse resp6 = new MockHttpServletResponse();
        sut.doFilter(req6, resp6, chain);

        assertThat(resp6.getStatus()).isEqualTo(429);
    }

    @Test
    void limitExceeded_retryAfterHeaderPresentAndPositive() throws Exception {
        for (int i = 0; i < 10; i++) {
            sut.doFilter(loginRequest("10.0.0.3"), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse resp429 = new MockHttpServletResponse();
        sut.doFilter(loginRequest("10.0.0.3"), resp429, chain);

        String retryAfter = resp429.getHeader("Retry-After");
        assertThat(retryAfter).isNotNull();
        assertThat(Long.parseLong(retryAfter)).isPositive();
    }

    @Test
    void differentIps_doNotShareBucket() throws Exception {
        for (int i = 0; i < 10; i++) {
            sut.doFilter(loginRequest("192.168.1.1"), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse respOtherIp = new MockHttpServletResponse();
        sut.doFilter(loginRequest("192.168.1.2"), respOtherIp, chain);

        assertThat(respOtherIp.getStatus()).isEqualTo(200);
    }

    @Test
    void redisUnavailable_requestIsAllowedThrough() throws Exception {
        sut = new RateLimitFilter(failingProxyManager(), new ObjectMapper(), "");

        MockHttpServletRequest  req  = loginRequest("10.0.0.99");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        sut.doFilter(req, resp, chain);

        assertThat(resp.getStatus()).isEqualTo(200);
        verify(chain).doFilter(any(), any());
    }

    // --- helpers ---

    @SuppressWarnings("unchecked")
    private ProxyManager<String> inMemoryProxyManager() {
        ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
        ProxyManager<String> pm = mock(ProxyManager.class, RETURNS_DEEP_STUBS);
        // Disambiguate the Supplier overload from the BucketConfiguration overload
        when(pm.builder().build(anyString(), any(Supplier.class))).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            @SuppressWarnings("unchecked")
            Supplier<BucketConfiguration> configSupplier = inv.getArgument(1);
            Bucket underlying = buckets.computeIfAbsent(key, k -> {
                BucketConfiguration cfg = configSupplier.get();
                var builder = Bucket.builder();
                for (Bandwidth bw : cfg.getBandwidths()) {
                    builder.addLimit(bw);
                }
                return builder.build();
            });
            BucketProxy proxy = mock(BucketProxy.class);
            lenient().when(proxy.tryConsumeAndReturnRemaining(anyLong()))
                .thenAnswer(i -> underlying.tryConsumeAndReturnRemaining(i.getArgument(0)));
            return proxy;
        });
        return pm;
    }

    @SuppressWarnings("unchecked")
    private ProxyManager<String> failingProxyManager() {
        ProxyManager<String> pm = mock(ProxyManager.class, RETURNS_DEEP_STUBS);
        when(pm.builder().build(anyString(), any(Supplier.class)))
            .thenThrow(new RuntimeException("Redis unavailable"));
        return pm;
    }

    private MockHttpServletRequest loginRequest(String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        req.setRemoteAddr(ip);
        return req;
    }

    private MockHttpServletRequest catalogRequest(String ip) {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/assets/catalog");
        req.setRemoteAddr(ip);
        return req;
    }
}
