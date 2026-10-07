package com.lukk.sky.gateway.config;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"local", "test"})
class CorsSingleValueTest {

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
    @DisplayName("preflight_whenOriginSupplied_thenSingleAllowOriginValue")
    void preflight_whenOriginSupplied_thenSingleAllowOriginValue() {
        var result = client.options().uri("/api/v1/offers")
                .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                .returnResult(Void.class);
        List<String> values =
                result.getResponseHeaders().get(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
        assertThat(values).containsExactly("http://localhost:4200");
    }

    @Test
    @DisplayName("simpleGet_whenOriginSupplied_thenSingleAllowOriginValue")
    void simpleGet_whenOriginSupplied_thenSingleAllowOriginValue() {
        var result = client.get().uri("/actuator/health")
                .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                .exchange()
                .expectStatus().isOk()
                .returnResult(Void.class);
        List<String> values =
                result.getResponseHeaders().get(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
        assertThat(values).containsExactly("http://localhost:4200");
    }
}
