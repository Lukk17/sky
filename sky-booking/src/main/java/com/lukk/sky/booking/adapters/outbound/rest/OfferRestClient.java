package com.lukk.sky.booking.adapters.outbound.rest;

import com.lukk.sky.booking.config.propertyBind.SkyConfigProperties;
import com.lukk.sky.booking.domain.exception.BookingException;
import com.lukk.sky.booking.domain.ports.outbound.RequestUriStrategy;
import com.lukk.sky.booking.domain.ports.outbound.RestClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.util.UUID;

/**
 * Synchronous implementation of {@link RestClient} that calls the offer service.
 *
 * <p>Retry (exponential backoff + jitter) and circuit-breaker behaviour are applied
 * by {@link OfferServiceCaller}, which carries the Resilience4j AOP annotations.
 * Splitting the call into a separate Spring bean is necessary because Resilience4j
 * relies on Spring AOP proxies: a method annotated in the same class and called via
 * {@code this.method()} bypasses the proxy and the annotations have no effect.
 *
 * <p>After all retry attempts are exhausted, a {@link ResourceAccessException} propagates
 * up from {@link OfferServiceCaller} and is converted here to a {@link BookingException}
 * so the domain layer never sees raw HTTP-client exceptions.
 */
@Service
@Slf4j
@Primary
@RequiredArgsConstructor
public class OfferRestClient implements RestClient {

    private final OfferServiceCaller offerServiceCaller;
    private final SkyConfigProperties skyConfigProperties;
    private final RequestUriStrategy requestUriStrategy;

    @Override
    public String requestOfferOwner(UUID offerId) {
        String endpoint = String.format("%s/%s/owner",
                skyConfigProperties.getOfferOwnerEndpoint(),
                offerId
        );
        String url = requestUriStrategy.createRestUrl(endpoint);
        log.info("Requesting owner of offer with ID: {}. URL: {}", offerId, url);

        try {
            return offerServiceCaller.callOfferService(url, offerId);
        } catch (ResourceAccessException ex) {
            log.error("Offer service call failed after all retries for offerId={}", offerId, ex);
            throw new BookingException("Could not resolve offer owner for offerId=" + offerId);
        }
    }
}
