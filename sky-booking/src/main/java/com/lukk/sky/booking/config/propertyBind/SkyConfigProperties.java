package com.lukk.sky.booking.config.propertyBind;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sky")
@Getter
@RequiredArgsConstructor
public class SkyConfigProperties {

    private final String offerServiceHostname;
    private final String offerServiceHostPort;
    private final String offerOwnerEndpoint;
}
