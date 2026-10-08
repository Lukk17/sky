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
    @DisplayName("accepts X-Sky-User set by the gateway")
    void beforeHandshake_whenGatewayHeaderPresent_thenUsesGatewayIdentity() {
        // given
        Map<String, Object> attributes = new HashMap<>();

        // when
        boolean result = interceptor.beforeHandshake(
                request(Map.of(GatewayUserHandshakeInterceptor.GATEWAY_USER_HEADER, List.of("gateway@test.com"))),
                response, handler, attributes);

        // then
        assertThat(result).isTrue();
        assertThat(attributes).containsEntry(
                GatewayUserHandshakeInterceptor.GATEWAY_USER_ATTRIBUTE, "gateway@test.com");
    }

    @Test
    @DisplayName("rejects spoofed email header when X-Sky-User is absent")
    void beforeHandshake_whenOnlySpoofedEmailPresent_thenSetsNothing() {
        // given
        Map<String, Object> attributes = new HashMap<>();

        // when
        boolean result = interceptor.beforeHandshake(
                request(Map.of("X-Auth-Request-Email", List.of("spoofed@evil.test"))),
                response, handler, attributes);

        // then
        assertThat(result).isTrue();
        assertThat(attributes).doesNotContainKey(GatewayUserHandshakeInterceptor.GATEWAY_USER_ATTRIBUTE);
    }

    @Test
    @DisplayName("sets no attribute when no identity header is present")
    void beforeHandshake_whenNoIdentityHeaders_thenSetsNothing() {
        // given
        Map<String, Object> attributes = new HashMap<>();

        // when
        boolean result = interceptor.beforeHandshake(request(Map.of()), response, handler, attributes);

        // then
        assertThat(result).isTrue();
        assertThat(attributes).doesNotContainKey(GatewayUserHandshakeInterceptor.GATEWAY_USER_ATTRIBUTE);
    }

    @Test
    @DisplayName("ignores blank gateway header and sets nothing")
    void beforeHandshake_whenGatewayHeaderBlank_thenSetsNothing() {
        // given
        Map<String, Object> attributes = new HashMap<>();

        // when
        boolean result = interceptor.beforeHandshake(
                request(Map.of(GatewayUserHandshakeInterceptor.GATEWAY_USER_HEADER, List.of("  "))),
                response, handler, attributes);

        // then
        assertThat(result).isTrue();
        assertThat(attributes).doesNotContainKey(GatewayUserHandshakeInterceptor.GATEWAY_USER_ATTRIBUTE);
    }

    @Test
    @DisplayName("ignores an unknown email header and keeps the gateway identity")
    void beforeHandshake_whenHeadersDisagree_thenPrefersGatewayIdentity() {
        // given
        Map<String, Object> attributes = new HashMap<>();

        // when
        boolean result = interceptor.beforeHandshake(
                request(Map.of(
                        GatewayUserHandshakeInterceptor.GATEWAY_USER_HEADER, List.of("gateway@test.com"),
                        "X-Auth-Request-Email", List.of("edge@test.com"))),
                response, handler, attributes);

        // then
        assertThat(result).isTrue();
        assertThat(attributes).containsEntry(
                GatewayUserHandshakeInterceptor.GATEWAY_USER_ATTRIBUTE, "gateway@test.com");
    }

    private static ServerHttpRequest request(Map<String, List<String>> headers) {
        ServerHttpRequest serverHttpRequest = mock(ServerHttpRequest.class);
        HttpHeaders httpHeaders = new HttpHeaders();
        headers.forEach(httpHeaders::put);
        when(serverHttpRequest.getHeaders()).thenReturn(httpHeaders);
        return serverHttpRequest;
    }
}
