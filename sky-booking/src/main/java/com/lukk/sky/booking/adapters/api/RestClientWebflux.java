package com.lukk.sky.booking.adapters.api;

import com.lukk.sky.booking.config.propertyBind.SkyConfigProperties;
import com.lukk.sky.booking.domain.exception.BookingException;
import com.lukk.sky.booking.domain.ports.api.RequestUriStrategy;
import com.lukk.sky.booking.domain.ports.api.RestClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * A {@link WebClient}-based implementation of the {@link RestClient} interface.
 * This service interacts with a RESTful service using {@link WebClient}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Primary
public class RestClientWebflux implements RestClient {

    private final WebClient webClient;
    private final SkyConfigProperties skyConfigProperties;
    private final RequestUriStrategy requestUriStrategy;

    /**
     * {@inheritDoc}
     * <p>This implementation does more than just requesting the offer owner:
     * <ul>
     *     <li>It constructs the URL for the request using {@link RequestUriStrategy}.</li>
     *     <li>It sends a GET request to the constructed URL using {@link WebClient}.</li>
     *     <li>It processes the response:
     *         <ul>
     *             <li>If the response status is error, it extracts the error message and propagates it as an exception.</li>
     *             <li>If the response status is successful, it extracts the owner from the response body.</li>
     *         </ul>
     *     </li>
     * </ul>
     */
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
                .exchangeToMono(RestClientWebflux::getClientResponseMono);
    }

    private static Mono<String> getClientResponseMono(ClientResponse clientResponse) {
        if (clientResponse.statusCode().isError()) {
            // clientResponse.bodyToMono(String.class) extracts the error message from response body
            return clientResponse.bodyToMono(String.class)
                    .flatMap(errorMessage -> Mono.error(new BookingException(errorMessage)));
        } else {
            return clientResponse.bodyToMono(String.class);
        }
    }
}
