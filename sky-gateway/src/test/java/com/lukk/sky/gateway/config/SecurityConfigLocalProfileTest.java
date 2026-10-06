package com.lukk.sky.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"local", "test"})
class SecurityConfigLocalProfileTest {

    @Value("${local.server.port}")
    private int port;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void prometheus_whenNoCredentialsSupplied_thenReturnsMetrics() {
        client.get().uri("/actuator/prometheus")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("jvm_memory_used_bytes"));
    }

    @Test
    void health_whenNoCredentialsSupplied_thenReturnsOk() {
        client.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/offers",
            "/api/v1/offers/11111111-1111-1111-1111-111111111111/owner",
            "/api/v1/search",
            "/api/v1/owner/offers",
            "/api/v1/bookings",
            "/api/v1/user/bookings",
            "/api/v1/messages"
    })
    void publishedApiPath_whenNoCredentialsSupplied_thenReachesItsRouteInsteadOfBeingRejected(String path) {
        client.get().uri(path)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/booking/swagger-ui.html",
            "/booking/swagger-ui/index.html",
            "/booking/v3/api-docs",
            "/offer/swagger-ui.html",
            "/offer/swagger-ui/index.html",
            "/offer/v3/api-docs",
            "/msg/swagger-ui.html",
            "/msg/swagger-ui/index.html",
            "/msg/v3/api-docs"
    })
    void publishedDocumentationPath_whenNoCredentialsSupplied_thenReachesItsRouteInsteadOfBeingRejected(String path) {
        client.get().uri(path)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/offer/swagger-ui/swagger-initializer.js",
            "/offer/swagger-ui/swagger-ui-bundle.js",
            "/offer/swagger-ui/swagger-ui-standalone-preset.js",
            "/offer/swagger-ui/swagger-ui.css",
            "/offer/swagger-ui/index.css",
            "/offer/swagger-ui/favicon-32x32.png",
            "/offer/v3/api-docs/swagger-config",
            "/offer/v3/api-docs/public"
    })
    void swaggerUiAssetOrConfigPath_whenRequested_thenReachesItsRouteInsteadOfBeingRejected(String path) {
        client.get().uri(path)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/booking/api/anything",
            "/offer/api/anything",
            "/msg/api/anything",
            "/booking/api/v1/bookings",
            "/offer/api/v1/offers",
            "/msg/api/v1/messages"
    })
    void retiredServicePrefix_whenRequested_thenMatchesNoRoute(String path) {
        client.get().uri(path)
                .exchange()
                .expectStatus().isNotFound();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/swagger-ui/index.html", "/v3/api-docs", "/v3/api-docs/swagger-config"})
    void documentationPathWithoutAServicePrefix_whenRequested_thenMatchesNoRoute(String path) {
        client.get().uri(path)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void notifyWebsocketHandshake_whenSockJsInfoRequested_thenMatchesTheNotifyRoute() {
        client.get().uri("/notifyWebsocket/info")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void notifyPrefix_whenRequested_thenMatchesNoRoute() {
        client.get().uri("/notify/anything")
                .exchange()
                .expectStatus().isNotFound();
    }
}
