package com.lukk.sky.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that ensures every request carries a correlation ID.
 *
 * <p>If the incoming request contains an {@code X-Correlation-Id} header the value is
 * reused; otherwise a random UUID is generated. The ID is stored in SLF4J {@link MDC}
 * under the key {@code correlationId} so every log line within the request scope
 * includes it automatically (configure your logback pattern with {@code %X{correlationId}}).
 * The same value is echoed back on the response via the same header.
 *
 * <p>The MDC entry is cleared in a {@code finally} block so pooled/virtual threads
 * never carry a stale value to the next request.
 *
 * <p>Registered automatically via {@link CorrelationIdAutoConfiguration} for all
 * {@code @ConditionalOnWebApplication} Spring Boot services that include sky-common
 * on their classpath. No per-service wiring is required.
 *
 * <p>TODO: when OpenTelemetry tracing is introduced, the OTel span context will provide
 * the trace/span ID. At that point this filter can either be removed in favour of the
 * OTel bridge or kept to populate the {@code X-Correlation-Id} response header from the
 * OTel {@code TraceId} so callers can correlate by the same key they already use.
 */
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
