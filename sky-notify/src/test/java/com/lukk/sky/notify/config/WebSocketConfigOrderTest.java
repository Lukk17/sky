package com.lukk.sky.notify.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WebSocketConfig: configurer ordering")
class WebSocketConfigOrderTest {

    @Test
    @DisplayName("runs first so the auth interceptor registers before Spring Security authorization")
    void getOrder_whenInspected_runsAtHighestPrecedence() {
        // given
        // when
        // then
        assertThat(java.util.Arrays.asList(WebSocketConfig.class.getInterfaces())).contains(Ordered.class);
        WebSocketConfig config = new WebSocketConfig(
                new WebSocketAuthChannelInterceptor(new NoOpJwtDecoder()),
                new GatewayUserHandshakeInterceptor(),
                "http://localhost:4200");
        assertThat(((Ordered) config).getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }

    @Test
    @DisplayName("replaces the STOMP CSRF check with a pass-through: auth is JWT or gateway session, not cookies")
    void csrfChannelInterceptor_whenConnectArrives_thenPassesThrough() {
        // given
        // when
        // then
        WebSocketConfig config = new WebSocketConfig(
                new WebSocketAuthChannelInterceptor(new NoOpJwtDecoder()),
                new GatewayUserHandshakeInterceptor(),
                "http://localhost:4200");

        org.springframework.messaging.support.ChannelInterceptor csrf = config.csrfChannelInterceptor();
        org.springframework.messaging.simp.stomp.StompHeaderAccessor accessor =
                org.springframework.messaging.simp.stomp.StompHeaderAccessor.create(
                        org.springframework.messaging.simp.stomp.StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        org.springframework.messaging.Message<byte[]> message = org.springframework.messaging.support.MessageBuilder
                .createMessage(new byte[0], accessor.getMessageHeaders());

        assertThat(csrf.preSend(message, org.mockito.Mockito.mock(org.springframework.messaging.MessageChannel.class)))
                .isSameAs(message);
    }

    private static final class NoOpJwtDecoder implements org.springframework.security.oauth2.jwt.JwtDecoder {
        @Override
        public org.springframework.security.oauth2.jwt.Jwt decode(String token) {
            throw new UnsupportedOperationException("test decoder never decodes");
        }
    }
}
