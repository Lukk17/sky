package com.lukk.sky.notify.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GatewayUserHandshakeInterceptor: edge identity extraction")
class GatewayUserHandshakeInterceptorTest {

    private final GatewayUserHandshakeInterceptor interceptor = new GatewayUserHandshakeInterceptor();
    private final WebSocketHandler handler = mock(WebSocketHandler.class);
    private final ServerHttpResponse response = mock(ServerHttpResponse.class);

    @Test
    @DisplayName("prefers X-Sky-User when both edge headers are present")
    void beforeHandshake_whenBothHeadersPresent_thenPrefersGatewayHeader() {
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(
                request(Map.of(
                        GatewayUserHandshakeInterceptor.GATEWAY_USER_HEADER, List.of("gateway@test.com"),
                        GatewayUserHandshakeInterceptor.EDGE_USER_HEADER, List.of("edge@test.com"))),
                response, handler, attributes);

        assertThat(result).isTrue();
        assertThat(attributes).containsEntry(
                GatewayUserHandshakeInterceptor.GATEWAY_USER_ATTRIBUTE, "gateway@test.com");
    }

    @Test
    @DisplayName("falls back to X-Auth-Request-Email when X-Sky-User is absent")
    void beforeHandshake_whenOnlyEdgeHeaderPresent_thenUsesEdgeIdentity() {
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(
                request(Map.of(GatewayUserHandshakeInterceptor.EDGE_USER_HEADER, List.of("edge@test.com"))),
                response, handler, attributes);

        assertThat(result).isTrue();
        assertThat(attributes).containsEntry(
                GatewayUserHandshakeInterceptor.GATEWAY_USER_ATTRIBUTE, "edge@test.com");
    }

    @Test
    @DisplayName("sets no attribute when neither edge header is present")
    void beforeHandshake_whenNoIdentityHeaders_thenSetsNothing() {
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(request(Map.of()), response, handler, attributes);

        assertThat(result).isTrue();
        assertThat(attributes).doesNotContainKey(GatewayUserHandshakeInterceptor.GATEWAY_USER_ATTRIBUTE);
    }

    @Test
    @DisplayName("ignores blank gateway header and falls back to the edge header")
    void beforeHandshake_whenGatewayHeaderBlank_thenFallsBack() {
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(
                request(Map.of(
                        GatewayUserHandshakeInterceptor.GATEWAY_USER_HEADER, List.of("  "),
                        GatewayUserHandshakeInterceptor.EDGE_USER_HEADER, List.of("edge@test.com"))),
                response, handler, attributes);

        assertThat(result).isTrue();
        assertThat(attributes).containsEntry(
                GatewayUserHandshakeInterceptor.GATEWAY_USER_ATTRIBUTE, "edge@test.com");
    }

    private static ServerHttpRequest request(Map<String, List<String>> headers) {
        ServerHttpRequest serverHttpRequest = mock(ServerHttpRequest.class);
        HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach(httpHeaders::put);
        when(serverHttpRequest.getHeaders()).thenReturn(httpHeaders);
        return serverHttpRequest;
    }
}
