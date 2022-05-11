package com.lukk.sky.zuulservice.config;

import lombok.Getter;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConstructorBinding
@ConfigurationProperties
@Getter
public class ConfigProperties {

    private final Spring spring;
    private final Server server;
    private final Logging logging;
    private final Eureka eureka;
    private final Management management;

    public ConfigProperties(Spring spring, Server server, Logging logging, Eureka eureka, Management management) {
        this.spring = spring;
        this.server = server;
        this.logging = logging;
        this.eureka = eureka;
        this.management = management;
    }

    @Value
    public static class Spring {

        Application application;

        @NestedConfigurationProperty
        String web;

        @NestedConfigurationProperty
        String mvc;

        @NestedConfigurationProperty
        String security;

        @NestedConfigurationProperty
        String jpa;

        @NestedConfigurationProperty
        String datasource;

        @Value
        public static class Application {
            String name;
        }
    }

    @Value
    public static class Management {
        Endpoints endpoints;

        @Value
        public static class Endpoints {
            Web web;

            @Value
            public static class Web {
                Exposure exposure;

                @Value
                public static class Exposure {
                    String include;
                }
            }
        }
    }

    @Value
    public static class Server {
        String port;
    }

    @Value
    public static class Logging {
        Level level;

        @Value
        public static class Level {
            Org org;

            @Value
            public static class Org {
                Springframework springframework;

                @Value
                public static class Springframework {
                    String web;
                }
            }
        }
    }

    @Value
    public static class Eureka {
        Client client;

        @Value
        public static class Client {
            ServiceUrl serviceUrl;

            @Value
            public static class ServiceUrl {
                String defaultZone;
            }
        }
    }
}