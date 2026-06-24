package com.lukk.sky.common.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code server.*} properties for startup logging.
 * Registered via {@link CommonConfigPropertiesAutoConfiguration}.
 */
@ConfigurationProperties(prefix = "server")
@Getter
@RequiredArgsConstructor
public class ServerConfigProperties {

    private final String port;
}
