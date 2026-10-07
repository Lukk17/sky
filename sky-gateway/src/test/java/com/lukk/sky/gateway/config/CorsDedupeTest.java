package com.lukk.sky.gateway.config;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"local", "test"})
class CorsDedupeTest {

    private static final HttpServer UPSTREAM = startUpstream();

    @Value("${local.server.port}")
    private int port;

    private WebTestClient client;

    @DynamicPropertySource
    static void upstreamUris(DynamicPropertyRegistry registry) {
        registry.add("sky-gateway.offer-uri", () -> baseUrl());
    }

    @AfterAll
    static void stopUpstream() {
        UPSTREAM.stop(0);
    }

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    @DisplayName("proxiedGet_whenUpstreamAlsoSendsAllowOrigin_thenSingleAllowOriginValue")
    void proxiedGet_whenUpstreamAlsoSendsAllowOrigin_thenSingleAllowOriginValue() {
        // when / then
        var result = client.get().uri("/api/v1/offers")
                .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                .exchange()
                .expectStatus().isOk()
                .returnResult(Void.class);
        List<String> values =
                result.getResponseHeaders().get(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
        assertThat(values).containsExactly("http://localhost:4200");
    }

    private static HttpServer startUpstream() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                String origin = exchange.getRequestHeaders().getFirst(HttpHeaders.ORIGIN);
                byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
                if (origin != null) {
                    exchange.getResponseHeaders().add(
                            HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                }
                exchange.getResponseHeaders().add("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, body.length);
                try (var out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException("cors dedupe stub could not bind", e);
        }
    }

    private static String baseUrl() {
        return "http://127.0.0.1:" + UPSTREAM.getAddress().getPort();
    }
}
