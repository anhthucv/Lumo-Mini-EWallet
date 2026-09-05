package com.chethu.paymentledgerservice.config;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Integer.MIN_VALUE)
public class RequestCorrelationFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";
    private static final int MAX_REQUEST_ID_LENGTH = 100;
    private static final Logger log = LoggerFactory.getLogger(RequestCorrelationFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = validRequestId(request.getHeader(REQUEST_ID_HEADER))
                ? request.getHeader(REQUEST_ID_HEADER).trim()
                : UUID.randomUUID().toString();
        long startedAt = System.nanoTime();
        MDC.put(MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            if (!isDocumentationRequest(request)) {
                log.info("request method={} path={} status={} durationMs={}", request.getMethod(),
                        request.getRequestURI(), response.getStatus(), durationMs);
            }
            MDC.remove(MDC_KEY);
        }
    }

    private boolean validRequestId(String requestId) {
        return requestId != null && requestId.length() <= MAX_REQUEST_ID_LENGTH
                && requestId.matches("[A-Za-z0-9._:-]+");
    }

    private boolean isDocumentationRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui");
    }
}
