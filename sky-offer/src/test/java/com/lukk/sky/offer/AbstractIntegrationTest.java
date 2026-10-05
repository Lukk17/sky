package com.lukk.sky.offer;

import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base for sky-offer integration tests. See {@code com.lukk.sky.booking.
 * AbstractIntegrationTest} for the rationale: same containers, same auto-wiring.
 *
 * <p>{@link TestSecurityConfig} provides a stub {@link org.springframework.security.oauth2.jwt.JwtDecoder}
 * that treats the bearer token value as the {@code email} claim.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestS3Config.class})
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES =
            // Labelled for pruneSkyTestcontainers cleanup.
            new PostgreSQLContainer(TestcontainersConfiguration.POSTGRES_IMAGE).withLabel("sky-testcontainer", "true");

    @ServiceConnection
    protected static final ConfluentKafkaContainer KAFKA =
            // Labelled for pruneSkyTestcontainers cleanup.
            new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0")).withLabel("sky-testcontainer", "true");

    static {
        POSTGRES.start();
        KAFKA.start();
    }
}
