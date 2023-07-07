package com.lukk.sky.offer.config.propertyBind;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(prefix = "spring")
@Getter
@RequiredArgsConstructor
public class SpringConfigProperties {

    private final Application application;
    private final @NestedConfigurationProperty String web;
    private final @NestedConfigurationProperty String mvc;
    private final @NestedConfigurationProperty String security;
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
