package com.lukk.sky.message.config.propertyBind;

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

    public record DataSource(String url, String driverClassName) {
    }

    public record Jpa(Hibernate hibernate) {
        public record Hibernate(String ddlAuto) {
        }
    }

    public record Application(String name) {
    }
}
