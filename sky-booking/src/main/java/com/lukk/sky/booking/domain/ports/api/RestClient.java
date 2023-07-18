package com.lukk.sky.booking.domain.ports.api;

import reactor.core.publisher.Mono;

/**
 * A strategy interface for requesting offer owners.
 * Provides operations for requesting the owner of a specific offer.
 */
public interface RestClient {
    /**
     * Requests the owner of an offer based on the provided offer ID.
     *
     * @param offerId The ID of the offer whose owner is to be requested.
     * @return A {@link Mono<String>} which, when subscribed, emits the offer owner.
     */
    Mono<String> requestOfferOwner(String offerId);
}
