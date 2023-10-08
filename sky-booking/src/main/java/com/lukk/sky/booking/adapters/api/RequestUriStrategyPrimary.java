package com.lukk.sky.booking.adapters.api;

import com.lukk.sky.booking.config.propertyBind.SkyConfigProperties;
import com.lukk.sky.booking.domain.ports.api.RequestUriStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Primary implementation of the {@link RequestUriStrategy}.
 * It uses {@link SkyConfigProperties} to get the hostname and port of the service, then appends the endpoint.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Primary
public class RequestUriStrategyPrimary implements RequestUriStrategy {

    private final SkyConfigProperties skyConfigProperties;

    /**
     * {@inheritDoc}
     * <p>This implementation does more than just creating a URL:
     * <ul>
     *     <li>It retrieves the hostname and port from {@link SkyConfigProperties}.</li>
     *     <li>It appends the provided endpoint to the base URL.</li>
     *     <li>It logs the created URL before returning it.</li>
     * </ul>
     */
    @Override
    public String createRestUrl(String endpoint) {
        String url;
        if (skyConfigProperties.getOfferServiceHostPort().isBlank()) {
            url = String.format("%s/%s",
                    skyConfigProperties.getOfferServiceHostname(),
                    endpoint
            );
        } else {
            url = String.format("%s:%s/%s",
                    skyConfigProperties.getOfferServiceHostname(),
                    skyConfigProperties.getOfferServiceHostPort(),
                    endpoint
            );
        }
        log.info("Creating default endpoint url: {}", url);

        return url;
    }
}
