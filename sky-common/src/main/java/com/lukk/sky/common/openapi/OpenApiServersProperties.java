package com.lukk.sky.common.openapi;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds {@code springdoc.servers} so each service declares its own addresses.
 * Registered via {@link OpenApiSecurityAutoConfiguration}.
 */
@ConfigurationProperties(prefix = "springdoc")
public record OpenApiServersProperties(List<ServerEntry> servers) {

    public OpenApiServersProperties {
        servers = servers == null ? List.of() : List.copyOf(servers);
    }

    public record ServerEntry(String url, String description) {
    }
}
