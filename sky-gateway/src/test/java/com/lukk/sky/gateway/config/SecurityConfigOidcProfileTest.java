package com.lukk.sky.gateway.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityConfigOidcProfileTest {

    private static final StubOidcProvider OIDC_PROVIDER = StubOidcProvider.start();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private RouteLocator routeLocator;

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

    @ParameterizedTest
    @ValueSource(strings = {
            "/booking/swagger-ui/index.html",
            "/booking/v3/api-docs",
            "/offer/swagger-ui/index.html",
            "/offer/v3/api-docs",
            "/msg/swagger-ui/index.html",
            "/msg/v3/api-docs"
    })
    void documentationPath_whenNoCredentialsSupplied_thenRedirectsToLoginInsteadOfServingTheDocs(String path) {
        client.get().uri(path)
                .exchange()
                .expectStatus().isFound()
                .expectHeader().value("Location", location ->
                        assertThat(location).contains("/oauth2/authorization/keycloak"));
    }

    @Test
    void routeTable_whenTheOidcDocumentIsActive_thenCarriesEveryDocumentationRouteTheLocalDocumentHas() {
        List<String> routeIds = routeLocator.getRoutes()
                .map(Route::getId)
                .collectList()
                .block();

        assertThat(routeIds).contains(
                "booking-swagger-route",
                "booking-api-docs-route",
                "offer-swagger-route",
                "offer-api-docs-route",
                "message-swagger-route",
                "message-api-docs-route");
    }
}
