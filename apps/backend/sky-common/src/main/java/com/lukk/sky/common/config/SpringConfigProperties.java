package com.lukk.sky.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring")
@Getter
@Setter
public class SpringConfigProperties {

    private Application application;
    private Jpa jpa;
    private DataSource datasource;
    private Kafka kafka;

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
