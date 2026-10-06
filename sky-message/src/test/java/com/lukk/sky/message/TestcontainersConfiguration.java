package com.lukk.sky.message;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Owns the PostgreSQL image tag for every sky-message test, and exposes it as a bean for
 * {@code @SpringBootTest} classes that need a live datasource without extending
 * {@link AbstractIntegrationTest}. Import via {@code @Import(TestcontainersConfiguration.class)}.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:17-alpine");

    static PostgreSQLContainer newPostgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return newPostgresContainer();
    }
}
