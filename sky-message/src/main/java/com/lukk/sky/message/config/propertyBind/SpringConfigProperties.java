package com.lukk.sky.message.config.propertyBind;

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

    public record DataSource(String url, String driverClassName) {
    }

    public record Jpa(Hibernate hibernate) {
        public record Hibernate(String ddlAuto) {
        }
    }

    public record Application(String name) {
    }
}
