package com.lukk.sky.notify.config.propertyBind;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring")
@Getter
@RequiredArgsConstructor
public class SpringConfigProperties {

    private final Application application;
    private final Kafka kafka;

    public record Application(String name) {
    }

    public record Kafka(String bootstrapServers, Admin admin) {

        public record Admin(String autoCreate) {
        }
    }

}
