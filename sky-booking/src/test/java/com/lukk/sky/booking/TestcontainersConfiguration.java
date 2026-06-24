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

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
                .withReuse(true);
    }
}
