package com.lukk.sky.message;

import com.lukk.sky.common.test.SharedTestSecurityConfig;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base for sky-message integration tests. sky-message uses PostgreSQL only, no Kafka.
 *
 * <p>{@link SharedTestSecurityConfig} provides a stub {@link org.springframework.security.oauth2.jwt.JwtDecoder}
 * that treats the bearer token value as the {@code email} claim.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
@ActiveProfiles("test")
@Import(SharedTestSecurityConfig.class)
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES = TestcontainersConfiguration.newPostgresContainer();
}
