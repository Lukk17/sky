package com.lukk.sky.common.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code management.*} properties for startup logging.
 * Registered via {@link CommonConfigPropertiesAutoConfiguration}.
 */
@ConfigurationProperties(prefix = "management")
@Getter
@RequiredArgsConstructor
public class ManagementConfigProperties {

    private final Endpoints endpoints;

    public record Endpoints(Web web) {
        public record Web(Exposure exposure) {
            public record Exposure(String include) {
            }
        }
    }
}
