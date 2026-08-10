package com.lukk.sky.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Auto-configuration that registers shared {@link org.springframework.boot.context.properties.ConfigurationProperties}
 * beans for every sky service.
 *
 * <p>Each service Application class injects these via constructor so startup
 * configuration details are logged uniformly. The per-service
 * {@code SpringConfigProperties} and any service-specific variants
 * ({@code SkyConfigProperties}) remain in the individual services because
 * they differ in structure across services.
 */
@AutoConfiguration
@EnableConfigurationProperties({
        ServerConfigProperties.class,
        ManagementConfigProperties.class,
        LoggingLvlConfigProperties.class
})
public class CommonConfigPropertiesAutoConfiguration {
}
