package com.lukk.sky.notify.config;

import com.lukk.sky.notify.AbstractIntegrationTest;
import com.lukk.sky.notify.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("WebSocket handshake origin check: full-stack integration tests")
@Import(TestSecurityConfig.class)
class WebSocketOriginIntegrationTest extends AbstractIntegrationTest {

    private static final String FOREIGN_ORIGIN = "https://attacker.example.com";
    private static final String ALLOWED_ORIGIN_HEADER = "Access-Control-Allow-Origin";
    private static final int HANDSHAKE_TIMEOUT_SECONDS = 15;
    private static final int OK = 200;
    private static final int FORBIDDEN = 403;

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @ParameterizedTest(name = "origin {0}")
    @ValueSource(strings = {
            "https://sky.luksarna.com",
            "https://skycloud.luksarna.com",
            "http://localhost:5777",
            "http://localhost:4200"})
    @DisplayName("the raw WebSocket handshake is upgraded when the browser origin is allowed")
    void rawHandshake_whenOriginIsAllowed_thenConnectionIsOpen(String origin) throws Exception {
        // when
        WebSocket socket = openRawWebSocket(origin);

        // then
        assertThat(socket.isInputClosed()).isFalse();
        assertThat(socket.isOutputClosed()).isFalse();

        socket.abort();
    }

    @Test
    @DisplayName("the raw WebSocket handshake is refused with 403 when the browser origin is not allowed")
    void rawHandshake_whenOriginIsNotAllowed_thenForbidden() {
        // when
        Throwable failure = catchThrowable(() -> openRawWebSocket(FOREIGN_ORIGIN));

        // then
        assertThat(failure).isInstanceOf(ExecutionException.class);
        assertThat(failure.getCause()).isInstanceOf(WebSocketHandshakeException.class);

        WebSocketHandshakeException handshakeFailure = (WebSocketHandshakeException) failure.getCause();

        assertThat(handshakeFailure.getResponse().statusCode()).isEqualTo(FORBIDDEN);
    }

    @ParameterizedTest(name = "origin {0}")
    @ValueSource(strings = {
            "https://sky.luksarna.com",
            "https://skycloud.luksarna.com",
            "http://localhost:5777",
            "http://localhost:4200"})
    @DisplayName("the SockJS fallback answers its info request when the browser origin is allowed")
    void sockJsInfo_whenOriginIsAllowed_thenPermitted(String origin) throws Exception {
        // when
        HttpResponse<String> response = getSockJsInfo(origin);

        // then
        assertThat(response.statusCode()).isEqualTo(OK);
        assertThat(response.headers().firstValue(ALLOWED_ORIGIN_HEADER)).contains(origin);
    }

    @Test
    @DisplayName("the SockJS fallback refuses its info request with 403 when the browser origin is not allowed")
    void sockJsInfo_whenOriginIsNotAllowed_thenForbidden() throws Exception {
        // when
        HttpResponse<String> response = getSockJsInfo(FOREIGN_ORIGIN);

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
