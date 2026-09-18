package com.lukk.sky.offer.config.propertyBind;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring")
@Getter
@RequiredArgsConstructor
public class SpringConfigProperties {

    private final Application application;
    private final Jpa jpa;
    private final DataSource datasource;
    private final Kafka kafka;

    public record DataSource(String url, String driverClassName) {
    }

    public record Jpa(Hibernate hibernate) {
        public record Hibernate(String ddlAuto) {
        }
    }

    public record Application(String name) {
    }

    public record Kafka(String bootstrapServers, Producer producer, Admin admin) {

        public record Producer(String clientId) {
        }

        public record Admin(String autoCreate) {
        }
    }

}
