package com.lukk.sky.gateway.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @Test
    void proxiedRoute_whenNoCredentialsSupplied_thenReachesRoutingInsteadOfBeingRejected() {
        client.get().uri("/booking/api/anything")
                .exchange()
                .expectStatus().is5xxServerError();
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
