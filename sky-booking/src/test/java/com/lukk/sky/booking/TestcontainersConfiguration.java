package com.lukk.sky.booking;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers configuration for {@code @SpringBootTest} classes that need a
 * live datasource but are not extending {@link AbstractIntegrationTest}. Import via
 * {@code @Import(TestcontainersConfiguration.class)} on the test class.
 *
 * <p>The container is static so it is started once per JVM and reused across all
 * importing test classes.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /**
     * Single source of truth for the container images used across this module's tests.
     */
    public static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:17-alpine");

    public static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:7.6.0");

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE);
    }
}
