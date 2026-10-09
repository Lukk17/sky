package com.lukk.sky.notify.config;

import com.lukk.sky.notify.AbstractIntegrationTest;
import com.lukk.sky.notify.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Notify security filter chain: full-stack integration tests")
@Import(TestSecurityConfig.class)
class NotifySecurityIntegrationTest extends AbstractIntegrationTest {

    private static final String BEARER_TOKEN = "integration-test-token";

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Test
    @DisplayName("health probe is reachable without a token")
    void healthProbe_whenAnonymous_thenPermitted() throws IOException, InterruptedException {
        // when
        HttpResponse<String> response = get("/actuator/health", null);

        // then
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("WebSocket handshake endpoint is reachable without a token, the STOMP CONNECT frame carries the JWT")
    void websocketEndpoint_whenAnonymous_thenPermitted() throws IOException, InterruptedException {
        // when
        HttpResponse<String> response = get("/notifyWebsocket/info", null);

        // then
        assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("every other endpoint is denied without a token")
    void metricsEndpoint_whenAnonymous_thenUnauthorized() throws IOException, InterruptedException {
        // when
        HttpResponse<String> response = get("/actuator/prometheus", null);

        // then
        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("an unknown path is denied without a token rather than leaking that it does not exist")
    void unknownPath_whenAnonymous_thenUnauthorized() throws IOException, InterruptedException {
        // when
        HttpResponse<String> response = get("/api/v1/there-is-no-such-endpoint", null);

        // then
        assertThat(response.statusCode()).isEqualTo(401);
    }

    @Test
    @DisplayName("a bearer token is accepted by the decoder the chain resolves from the context")
    void metricsEndpoint_whenBearerTokenPresent_thenAuthorized() throws IOException, InterruptedException {
        // when
        HttpResponse<String> response = get("/actuator/prometheus", BEARER_TOKEN);

        // then
        assertThat(response.statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> get(String path, String bearerToken) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET();

        if (bearerToken != null) {
            request.header("Authorization", "Bearer " + bearerToken);
        }

        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
