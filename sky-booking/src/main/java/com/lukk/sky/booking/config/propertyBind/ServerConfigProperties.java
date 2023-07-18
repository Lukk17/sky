package com.lukk.sky.booking.config.propertyBind;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "server")
@Getter
@RequiredArgsConstructor
public class ServerConfigProperties {

    private final String port;

}
