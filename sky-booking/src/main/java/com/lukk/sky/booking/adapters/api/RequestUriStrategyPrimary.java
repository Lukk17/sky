package com.lukk.sky.booking.adapters.api;

import com.lukk.sky.booking.config.propertyBind.SkyConfigProperties;
import com.lukk.sky.booking.domain.ports.api.RequestUriStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestUriStrategyPrimary implements RequestUriStrategy {

    private final SkyConfigProperties skyConfigProperties;

    @Override
    public String createRestUrl(String endpoint) {
        String url = String.format("%s:%s/%s",
                skyConfigProperties.getOfferServiceAddress(),
                skyConfigProperties.getOfferServicePort(),
                endpoint
        );
        log.info("Creating default endpoint url: {}", url);

        return url;
    }
}
