package com.lukk.sky.booking.domain.ports.api;

import reactor.core.publisher.Mono;

public interface RestClient {
    Mono<String> requestOfferOwner(String offerId);
}
