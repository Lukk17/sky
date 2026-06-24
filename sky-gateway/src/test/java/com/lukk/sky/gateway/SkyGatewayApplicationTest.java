package com.lukk.sky.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: Spring context loads without errors in the default (no-auth) mode.
 * No external services are required — routes reference URIs that are never
 * actually called during context startup.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SkyGatewayApplicationTest {

    @Test
    void contextLoads() {
        // If the Spring context fails to start, this test fails.
    }
}
