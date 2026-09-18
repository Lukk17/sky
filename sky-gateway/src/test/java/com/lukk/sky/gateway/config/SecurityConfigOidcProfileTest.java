package com.lukk.sky.gateway.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityConfigOidcProfileTest {

    private static final StubOidcProvider OIDC_PROVIDER = StubOidcProvider.start();

    @Value("${local.server.port}")
    private int port;

    private WebTestClient client;

    @DynamicPropertySource
    static void keycloakProperties(DynamicPropertyRegistry registry) {
        registry.add("KEYCLOAK_ISSUER_URI", OIDC_PROVIDER::issuerUri);
        registry.add("KEYCLOAK_CLIENT_ID", () -> "sky-gateway");
        registry.add("KEYCLOAK_CLIENT_SECRET", () -> "stub-client-secret");
    }

    @AfterAll
    static void stopOidcProvider() {
        OIDC_PROVIDER.close();
    }

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void prometheus_whenNoCredentialsSupplied_thenRedirectsToLoginInsteadOfExposingMetrics() {
        client.get().uri("/actuator/prometheus")
                .exchange()
                .expectStatus().isFound()
                .expectHeader().value("Location", location ->
                        assertThat(location).contains("/oauth2/authorization/keycloak"))
                .expectBody().isEmpty();
    }

    @Test
    void health_whenNoCredentialsSupplied_thenReturnsOk() {
        client.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void livenessProbe_whenNoCredentialsSupplied_thenReturnsOk() {
        client.get().uri("/actuator/health/liveness")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void readinessProbe_whenNoCredentialsSupplied_thenReturnsOk() {
        client.get().uri("/actuator/health/readiness")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void info_whenNoCredentialsSupplied_thenReturnsOk() {
        client.get().uri("/actuator/info")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void proxiedRoute_whenNoCredentialsSupplied_thenRedirectsToLogin() {
        client.get().uri("/api/v1/bookings")
                .exchange()
                .expectStatus().isFound();
    }
}
