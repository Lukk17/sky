package com.lukk.sky.booking.adapters.api;

import com.lukk.sky.booking.config.propertyBind.SkyConfigProperties;
import com.lukk.sky.booking.domain.ports.api.RequestUriStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RequestUriStrategyK8s implements RequestUriStrategy {

    private final SkyConfigProperties skyConfigProperties;

    @Override
    public String createRestUrl(String endpoint) {
        String url = String.format("%s/%s",
                skyConfigProperties.getOfferServiceAddress(),
                endpoint
        );
        log.info("Creating K8S endpoint url: {}", url);

        return url;
    }
}
