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

    @SuppressWarnings("unused")
    public String fallback(String url, String offerId, CallNotPermittedException ex) {
        log.warn("Offer service circuit breaker OPEN — failing fast for offerId={}", offerId);
        throw new BookingException("Offer service unavailable for offerId=" + offerId);
    }
}
