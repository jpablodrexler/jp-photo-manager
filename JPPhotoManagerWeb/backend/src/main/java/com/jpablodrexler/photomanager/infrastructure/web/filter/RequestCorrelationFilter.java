package com.jpablodrexler.photomanager.infrastructure.web.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.UUID;

/**
 * Tags every HTTP request with a {@code requestId} UUID and the authenticated {@code username}
 * in SLF4J MDC so all log lines emitted while the request is being processed can be correlated
 * (the {@code logstash-logback-encoder} already configured in {@code logback-spring.xml}
 * automatically includes MDC entries in the JSON log output). The {@code requestId} is also
 * echoed back as an {@code X-Request-ID} response header so the Angular frontend can surface it
 * to the user for error reports.
 *
 * <p>The {@code requestId} is always generated server-side rather than accepted from an incoming
 * {@code X-Request-ID} header — trusting a caller-supplied value would let clients inject
 * arbitrary content into structured log fields.
 */
public class RequestCorrelationFilter implements Filter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final String USERNAME_MDC_KEY = "username";
    private static final String ANONYMOUS_USERNAME = "anonymous";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String requestId = UUID.randomUUID().toString();
        String username = resolveUsername();

        try {
            MDC.put(REQUEST_ID_MDC_KEY, requestId);
            MDC.put(USERNAME_MDC_KEY, username);

            ((HttpServletResponse) response).setHeader(REQUEST_ID_HEADER, requestId);

            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String resolveUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return ANONYMOUS_USERNAME;
        }
        return authentication.getName();
    }
}
