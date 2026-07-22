package com.jpablodrexler.photomanager.infrastructure.web.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RequestCorrelationFilterTest {

    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");

    @Mock FilterChain chain;

    RequestCorrelationFilter sut;

    @BeforeEach
    void setUp() {
        sut = new RequestCorrelationFilter();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void doFilter_anyRequest_setsXRequestIdHeaderWithValidUuid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/assets");
        MockHttpServletResponse response = new MockHttpServletResponse();

        sut.doFilter(request, response, chain);

        String requestId = response.getHeader("X-Request-ID");
        assertThat(requestId).isNotNull();
        assertThat(UUID.fromString(requestId)).isNotNull();
        assertThat(UUID_PATTERN.matcher(requestId).matches()).isTrue();
    }

    @Test
    void doFilter_duringChainExecution_mdcRequestIdMatchesResponseHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/assets");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] mdcRequestIdDuringChain = new String[1];
        doAnswer(invocation -> {
            mdcRequestIdDuringChain[0] = MDC.get("requestId");
            return null;
        }).when(chain).doFilter(request, response);

        sut.doFilter(request, response, chain);

        assertThat(mdcRequestIdDuringChain[0]).isEqualTo(response.getHeader("X-Request-ID"));
    }

    @Test
    void doFilter_afterChainCompletes_mdcIsCleared() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/assets");
        MockHttpServletResponse response = new MockHttpServletResponse();

        sut.doFilter(request, response, chain);

        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("username")).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_unauthenticatedRequest_setsAnonymousUsernameInMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] mdcUsernameDuringChain = new String[1];
        doAnswer(invocation -> {
            mdcUsernameDuringChain[0] = MDC.get("username");
            return null;
        }).when(chain).doFilter(request, response);

        sut.doFilter(request, response, chain);

        assertThat(mdcUsernameDuringChain[0]).isEqualTo("anonymous");
    }

    @Test
    void doFilter_authenticatedRequest_setsPrincipalNameAsUsernameInMdc() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("jdoe", null));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/assets");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] mdcUsernameDuringChain = new String[1];
        doAnswer(invocation -> {
            mdcUsernameDuringChain[0] = MDC.get("username");
            return null;
        }).when(chain).doFilter(request, response);

        sut.doFilter(request, response, chain);

        assertThat(mdcUsernameDuringChain[0]).isEqualTo("jdoe");
    }

    @Test
    void doFilter_chainThrows_stillClearsMdc() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/assets");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> {
            doAnswer(invocation -> {
                throw new RuntimeException("downstream failure");
            }).when(chain).doFilter(request, response);
            sut.doFilter(request, response, chain);
        }).isInstanceOf(RuntimeException.class);

        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("username")).isNull();
    }
}
