package com.lukk.sky.booking.adapters.outbound.rest;

import com.lukk.sky.booking.domain.exception.OfferNotFoundException;
import com.lukk.sky.booking.domain.exception.OfferServiceBadResponseException;
import com.lukk.sky.booking.domain.exception.OfferServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OfferServiceCaller {

    private final RestClient restClient;

    @Retry(name = "offerService")
    @CircuitBreaker(name = "offerService", fallbackMethod = "fallback")
    public String callOfferService(String url, UUID offerId) {
        RestClient.RequestHeadersSpec<?> spec = restClient.get().uri(url);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            spec = spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtAuthentication.getToken().getTokenValue());
        }

        return spec
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                    if (resp.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                        throw new OfferNotFoundException("Offer not found for offerId=" + offerId);
                    }
                    throw new OfferServiceBadResponseException("Could not resolve offer owner for offerId=" + offerId);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                    throw new ResourceAccessException("Offer service 5xx for offerId=" + offerId);
                })
                .body(String.class);
    }

    public String fallback(String url, UUID offerId, CallNotPermittedException ex) {
        log.warn("Offer service circuit breaker OPEN, failing fast for offerId={}", offerId);

        throw new OfferServiceUnavailableException("Offer service unavailable for offerId=" + offerId, ex);
    }
}
