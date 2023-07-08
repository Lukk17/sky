package com.lukk.sky.booking.adapters.api;

import com.lukk.sky.booking.config.propertyBind.SkyConfigProperties;
import com.lukk.sky.booking.domain.exception.BookingException;
import com.lukk.sky.booking.domain.ports.api.RestClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestClientWebflux implements RestClient {

    private final WebClient webClient;
    private final SkyConfigProperties skyConfigProperties;

    @Override
    public Mono<String> requestOfferOwner(String offerId) {

        String url = String.format("%s:%s/%s/%s",
                skyConfigProperties.getOfferServiceAddress(),
                skyConfigProperties.getOfferServicePort(),
                skyConfigProperties.getOfferOwnerEndpoint(),
                offerId
        );

        return webClient.get()
                .uri(url)
                .exchangeToMono(clientResponse -> {
                    if (clientResponse.statusCode().isError()) {
                        // clientResponse.bodyToMono(String.class) extracts the error message from response body
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(errorMessage -> Mono.error(new BookingException(errorMessage)));
                    } else {
                        return clientResponse.bodyToMono(String.class);
                    }
                });
    }
}
