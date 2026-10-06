package com.lukk.sky.notify.config;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("WebSocketAuthChannelInterceptor: STOMP CONNECT authentication")
class WebSocketAuthChannelInterceptorTest {

    private static final String VALID_TOKEN = "valid-token";
    private static final String PRINCIPAL = "user@test.com";

    private final JwtDecoder jwtDecoder = mock(JwtDecoder.class);
    private final MessageChannel channel = mock(MessageChannel.class);
    private final WebSocketAuthChannelInterceptor interceptor = new WebSocketAuthChannelInterceptor(jwtDecoder);

    @Test
    @DisplayName("attaches the JWT principal to the session when CONNECT carries a valid bearer token")
    void preSend_whenConnectCarriesValidToken_thenAttachesAuthenticatedPrincipal() {
        // given
        given(jwtDecoder.decode(VALID_TOKEN)).willReturn(jwt());
        StompHeaderAccessor accessor = connectAccessor("Bearer " + VALID_TOKEN);

        // when
        interceptor.preSend(message(accessor), channel);

        // then
        assertThat(accessor.getUser()).isInstanceOf(JwtAuthenticationToken.class);
        assertThat(accessor.getUser().getName()).isEqualTo(PRINCIPAL);
    }

    @Test
    @DisplayName("rejects CONNECT without an Authorization header")
    void preSend_whenConnectHasNoAuthorizationHeader_thenRejects() {
        // given
        Message<byte[]> message = message(connectAccessor(null));

        // when / then
        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Missing Bearer token");
    }

    @Test
    @DisplayName("rejects CONNECT whose Authorization header is not a bearer token")
    void preSend_whenConnectHasMalformedAuthorizationHeader_thenRejects() {
        // given
        Message<byte[]> message = message(connectAccessor("Basic dXNlcjpwYXNz"));

        // when / then
        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("rejects CONNECT when the token fails validation")
    void preSend_whenTokenIsInvalid_thenRejects() {
        // given
        given(jwtDecoder.decode("expired-token")).willThrow(new JwtException("expired"));
        Message<byte[]> message = message(connectAccessor("Bearer expired-token"));

        // when / then
        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid JWT");
    }

    @Test
    @DisplayName("passes non-CONNECT frames through untouched")
    void preSend_whenFrameIsNotConnect_thenPassesThrough() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = message(accessor);

        // when
        Message<?> forwarded = interceptor.preSend(message, channel);

        // then
        assertThat(forwarded).isSameAs(message);
        assertThat(accessor.getUser()).isNull();
    }

    @Test
    @DisplayName("the config package is null-marked so framework overrides keep their non-null contract")
    void configPackage_whenInspected_thenIsNullMarked() {
        assertThat(WebSocketAuthChannelInterceptor.class.getPackage().getAnnotation(NullMarked.class)).isNotNull();
    }

    private StompHeaderAccessor connectAccessor(String authorizationHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        if (authorizationHeader != null) {
            accessor.setNativeHeader("Authorization", authorizationHeader);
        }

        return accessor;
    }

    private Message<byte[]> message(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Jwt jwt() {
        return Jwt.withTokenValue(VALID_TOKEN)
                .header("alg", "RS256")
                .claim("sub", PRINCIPAL)
                .build();
    }
}
