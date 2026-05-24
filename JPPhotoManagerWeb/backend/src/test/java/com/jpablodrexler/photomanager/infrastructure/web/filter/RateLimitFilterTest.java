package com.jpablodrexler.photomanager.infrastructure.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock FilterChain chain;

    RateLimitFilter sut;

    @BeforeEach
    void setUp() {
        sut = new RateLimitFilter(new ObjectMapper());
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
