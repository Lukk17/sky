package com.lukk.sky.common.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code logging.*} properties for startup logging.
 * Registered via {@link CommonConfigPropertiesAutoConfiguration}.
 *
 * <p>The {@code hibernate} field in the {@code Org} record is nullable:
 * sky-notify does not set {@code logging.level.org.hibernate}, so the
 * field will be {@code null} there. Callers in sky-notify must not
 * invoke {@code .hibernate()} without a null-check.
 */
@ConfigurationProperties(prefix = "logging")
@Getter
@RequiredArgsConstructor
public class LoggingLvlConfigProperties {

    private final Level level;

    public record Level(Org org) {
        public record Org(Springframework springframework, String hibernate) {
            public record Springframework(String web) {
            }
        }
    }
}
