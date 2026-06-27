package com.lukk.sky.common.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static com.lukk.sky.common.web.CorrelationIdFilter.CORRELATION_ID_HEADER;
import static com.lukk.sky.common.web.CorrelationIdFilter.MDC_KEY;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CorrelationIdFilter")
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.remove(MDC_KEY);
    }

    @Test
    @DisplayName("generates a UUID correlation ID when the request carries no X-Correlation-Id header")
    void generatesCorrelationId_whenHeaderAbsent() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        String responseHeader = response.getHeader(CORRELATION_ID_HEADER);
        assertThat(responseHeader)
                .as("response must carry X-Correlation-Id")
                .isNotNull()
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("reuses an existing X-Correlation-Id header when one is present on the request")
    void reusesCorrelationId_whenHeaderPresent() throws ServletException, IOException {
        String incoming = "test-correlation-id-42";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CORRELATION_ID_HEADER, incoming);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(CORRELATION_ID_HEADER))
                .as("response must echo the incoming correlation ID unchanged")
                .isEqualTo(incoming);
    }

    @Test
    @DisplayName("populates MDC with the correlation ID during filter execution")
    void populatesMdc_duringFilterExecution() throws ServletException, IOException {
        String incoming = "mdc-check-id";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CORRELATION_ID_HEADER, incoming);
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] capturedMdc = new String[1];
        Filter mdcCapture = new Filter() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
                    throws IOException, ServletException {
                capturedMdc[0] = MDC.get(MDC_KEY);
            }
        };
        MockFilterChain chain = new MockFilterChain(new jakarta.servlet.http.HttpServlet() {
        }, mdcCapture);

        filter.doFilter(request, response, chain);

        assertThat(capturedMdc[0])
                .as("MDC must hold the correlation ID while the filter chain executes")
                .isEqualTo(incoming);
    }

    @Test
    @DisplayName("clears the MDC entry after the request completes")
    void clearsMdc_afterRequestCompletes() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CORRELATION_ID_HEADER, "cleanup-test-id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(MDC.get(MDC_KEY))
                .as("MDC must be cleared after the filter chain completes")
                .isNull();
    }
}
