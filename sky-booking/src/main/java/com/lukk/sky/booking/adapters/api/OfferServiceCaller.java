package com.lukk.sky.booking.adapters.api;

import com.lukk.sky.booking.domain.exception.BookingException;
import com.lukk.sky.common.web.CorrelationIdFilter;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Spring-managed component that performs the single outbound HTTP call to the offer service.
 *
 * <p>Keeping the Resilience4j annotations here (not on {@link OfferRestClient}) is required
 * because Resilience4j uses Spring AOP proxies: the annotated method must be invoked
 * <em>through</em> the Spring proxy, i.e. via a call on a different bean. A method annotated
 * inside the same class and called via {@code this.method()} bypasses the proxy entirely and
 * the annotations have no effect.
 *
 * <p>Annotation chain: {@code @Retry} is outer, {@code @CircuitBreaker} is inner. When a 5xx
 * triggers a {@link ResourceAccessException} inside the circuit breaker, the circuit breaker
 * records the failure and rethrows — the retry sees a retryable exception and repeats. When
 * the circuit transitions to OPEN, Resilience4j throws {@link CallNotPermittedException} before
 * invoking the method at all; the fallback matches that specific type and converts it to a
 * {@link BookingException} so callers receive a fast, typed failure instead of a raw library
 * exception.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OfferServiceCaller {

    private final RestClient restClient;

    @Retry(name = "offerService")
    @CircuitBreaker(name = "offerService", fallbackMethod = "fallback")
    public String callOfferService(String url, String offerId) {
        String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);

        RestClient.RequestHeadersSpec<?> spec = restClient.get().uri(url);
        if (correlationId != null) {
            spec = spec.header(CorrelationIdFilter.CORRELATION_ID_HEADER, correlationId);
        }

        return spec
                .retrieve()
                .onStatus(
                        HttpStatusCode::is4xxClientError,
                        (req, resp) -> {
                            if (resp.getStatusCode().value() == 404) {
                                throw new BookingException("Offer not found for offerId=" + offerId);
                            }
                            throw new BookingException(
                                    "Could not resolve offer owner for offerId=" + offerId);
                        }
                )
                .onStatus(
                        HttpStatusCode::is5xxServerError,
                        (req, resp) -> {
                            throw new ResourceAccessException(
                                    "Offer service 5xx for offerId=" + offerId);
                        }
                )
                .body(String.class);
    }

    /**
     * Called only when the circuit breaker is OPEN and rejects the request without calling the
     * underlying method. Throws a {@link BookingException} so the caller receives a typed failure
     * message rather than the library-internal {@link CallNotPermittedException}.
     */
    @SuppressWarnings("unused")
    public String fallback(String url, String offerId, CallNotPermittedException ex) {
        log.warn("Offer service circuit breaker OPEN — failing fast for offerId={}", offerId);
        throw new BookingException("Offer service unavailable for offerId=" + offerId);
    }
}
