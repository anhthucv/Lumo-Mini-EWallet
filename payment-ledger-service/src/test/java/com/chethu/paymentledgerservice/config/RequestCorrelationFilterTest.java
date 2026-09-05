package com.chethu.paymentledgerservice.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

class RequestCorrelationFilterTest {
    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void missingRequestId_generatesResponseIdAndClearsMdc() throws Exception {
        MockHttpServletRequest request = request();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> observedId = new AtomicReference<>();

        filter.doFilter(request, response, chainThatCapturesMdc(observedId));

        String responseId = response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER);
        assertTrue(responseId.matches("[0-9a-f-]{36}"));
        assertEquals(responseId, observedId.get());
        assertNull(MDC.get(RequestCorrelationFilter.MDC_KEY));
    }

    @Test
    void validRequestId_isReusedAndAvailableInMdc() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "client.req-42");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> observedId = new AtomicReference<>();

        filter.doFilter(request, response, chainThatCapturesMdc(observedId));

        assertEquals("client.req-42", response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER));
        assertEquals("client.req-42", observedId.get());
        assertNull(MDC.get(RequestCorrelationFilter.MDC_KEY));
    }

    @Test
    void invalidRequestId_isReplacedSafely() throws Exception {
        MockHttpServletRequest request = request();
        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "bad value");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (request1, response1) -> { });

        assertNotEquals("bad value", response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER));
        assertTrue(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER).matches("[0-9a-f-]{36}"));
        assertNull(MDC.get(RequestCorrelationFilter.MDC_KEY));
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        return request;
    }

    private FilterChain chainThatCapturesMdc(AtomicReference<String> observedId) {
        return (request, response) -> observedId.set(MDC.get(RequestCorrelationFilter.MDC_KEY));
    }
}
