package com.lukk.sky.notify.config;

import com.lukk.sky.notify.AbstractIntegrationTest;
import com.lukk.sky.notify.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("WebSocket handshake origin check: full-stack integration tests")
@Import(TestSecurityConfig.class)
@TestPropertySource(properties =
        "sky.crossOrigin.allowed=" + WebSocketOriginIntegrationTest.CONFIGURED_ORIGINS)
class WebSocketOriginIntegrationTest extends AbstractIntegrationTest {

    static final String FIRST_CONFIGURED_ORIGIN = "https://frontend.sky.test";
    static final String SECOND_CONFIGURED_ORIGIN = "http://localhost:4200";
    static final String CONFIGURED_ORIGINS = FIRST_CONFIGURED_ORIGIN + "," + SECOND_CONFIGURED_ORIGIN;

    private static final String FOREIGN_ORIGIN = "https://attacker.example.com";
    private static final String FORMERLY_HARDCODED_ORIGIN = "https://skycloud.luksarna.com";
    private static final String ALLOWED_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    private static final int HANDSHAKE_TIMEOUT_SECONDS = 15;
    private static final int OK = 200;
    private static final int FORBIDDEN = 403;

    @LocalServerPort
    private int port;

    @Value("${sky.crossOrigin.allowed}")
    private String boundAllowedOrigins;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    static Stream<String> configuredOrigins() {
        return Stream.of(CONFIGURED_ORIGINS.split(","));
    }

    @Test
    @DisplayName("the endpoints answer on the origins the bound property names, not on a list compiled into the code")
    void allowedOrigins_whenInspected_thenComeFromTheBoundProperty() {
        // then
        assertThat(boundAllowedOrigins).isEqualTo(CONFIGURED_ORIGINS);
        assertThat(List.of(boundAllowedOrigins.split(",")))
                .doesNotContain(FORMERLY_HARDCODED_ORIGIN, FOREIGN_ORIGIN);
    }

    @ParameterizedTest(name = "origin {0}")
    @MethodSource("configuredOrigins")
    @DisplayName("the raw WebSocket handshake is upgraded when the browser origin is allowed")
    void rawHandshake_whenOriginIsAllowed_thenConnectionIsOpen(String origin) throws Exception {
        // when
        WebSocket socket = openRawWebSocket(origin);

        // then
        assertThat(socket.isInputClosed()).isFalse();
        assertThat(socket.isOutputClosed()).isFalse();

        socket.abort();
    }

    @ParameterizedTest(name = "origin {0}")
    @ValueSource(strings = {FOREIGN_ORIGIN, FORMERLY_HARDCODED_ORIGIN})
    @DisplayName("the raw WebSocket handshake is refused with 403 when the browser origin is not on the configured list")
    void rawHandshake_whenOriginIsNotAllowed_thenForbidden(String origin) {
        // when
        Throwable failure = catchThrowable(() -> openRawWebSocket(origin));

        // then
        assertThat(failure).isInstanceOf(ExecutionException.class);
        assertThat(failure.getCause()).isInstanceOf(WebSocketHandshakeException.class);

        WebSocketHandshakeException handshakeFailure = (WebSocketHandshakeException) failure.getCause();

        assertThat(handshakeFailure.getResponse().statusCode()).isEqualTo(FORBIDDEN);
    }

    @ParameterizedTest(name = "origin {0}")
    @MethodSource("configuredOrigins")
    @DisplayName("the SockJS fallback answers its info request when the browser origin is allowed")
    void sockJsInfo_whenOriginIsAllowed_thenPermitted(String origin) throws Exception {
        // when
        HttpResponse<String> response = getSockJsInfo(origin);

        // then
        assertThat(response.statusCode()).isEqualTo(OK);
        assertThat(response.headers().firstValue(ALLOWED_ORIGIN_HEADER)).contains(origin);
    }

    @ParameterizedTest(name = "origin {0}")
    @ValueSource(strings = {FOREIGN_ORIGIN, FORMERLY_HARDCODED_ORIGIN})
    @DisplayName("the SockJS fallback refuses its info request with 403 when the browser origin is not on the configured list")
    void sockJsInfo_whenOriginIsNotAllowed_thenForbidden(String origin) throws Exception {
        // when
        HttpResponse<String> response = getSockJsInfo(origin);

        // then
        assertThat(response.statusCode()).isEqualTo(FORBIDDEN);
        assertThat(response.headers().firstValue(ALLOWED_ORIGIN_HEADER)).isEmpty();
    }

    private WebSocket openRawWebSocket(String origin) throws Exception {
        return httpClient.newWebSocketBuilder()
                .header("Origin", origin)
                .buildAsync(URI.create("ws://localhost:" + port + "/notifyWebsocket"), new WebSocket.Listener() {
                })
                .get(HANDSHAKE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    private HttpResponse<String> getSockJsInfo(String origin) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/notifyWebsocket/info"))
                .header("Origin", origin)
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
