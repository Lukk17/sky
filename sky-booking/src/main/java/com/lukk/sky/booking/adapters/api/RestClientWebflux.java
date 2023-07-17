package com.lukk.sky.booking.adapters.api;

import com.lukk.sky.booking.config.propertyBind.SkyConfigProperties;
import com.lukk.sky.booking.domain.exception.BookingException;
import com.lukk.sky.booking.domain.ports.api.RequestUriStrategy;
import com.lukk.sky.booking.domain.ports.api.RestClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
@Primary
public class RestClientWebflux implements RestClient {

    private final WebClient webClient;
    private final SkyConfigProperties skyConfigProperties;
    private final RequestUriStrategy requestUriStrategy;

    @Override
    public Mono<String> requestOfferOwner(String offerId) {

        String endpoint = String.format("%s/%s",
                skyConfigProperties.getOfferOwnerEndpoint(),
                offerId
        );

        String url = requestUriStrategy.createRestUrl(endpoint);

        log.info("Sending request for owner of offer with ID: {}. Rest url: {}", offerId, url);

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
