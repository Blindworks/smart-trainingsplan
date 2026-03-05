package com.trainingsplan.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void doFilterInternal_reusesIncomingCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "corr-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                assertEquals("corr-123", MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
            }
        };

        filter.doFilter(request, response, chain);

        assertEquals("corr-123", response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER));
        assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
    }

    @Test
    void doFilterInternal_generatesCorrelationIdWhenMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = new FilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                String mdcId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
                assertNotNull(mdcId);
                assertEquals(mdcId, ((MockHttpServletResponse) res).getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER));
            }
        };

        filter.doFilter(request, response, chain);

        assertNotNull(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER));
        assertNull(MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY));
    }
}
