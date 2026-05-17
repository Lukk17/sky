package com.lukk.sky.notify.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Validates the JWT carried in the STOMP {@code CONNECT} frame's {@code Authorization}
 * header. On success, the resolved {@link Authentication} is attached to the message
 * so {@link org.springframework.messaging.simp.SimpMessagingTemplate#convertAndSendToUser}
 * can address per-user destinations.
 *
 * <p>Failure modes:
 * <ul>
 *   <li>Missing or non-Bearer Authorization → {@link BadCredentialsException} (STOMP ERROR).</li>
 *   <li>Invalid / expired / wrong-issuer JWT → {@link BadCredentialsException}.</li>
 * </ul>
 *
 * @see AuthenticationManager (unused by design — we want a hard fail rather than chained authn)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader("Authorization");
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                log.warn("WS CONNECT rejected: missing or malformed Authorization header");
                throw new BadCredentialsException("Missing Bearer token on STOMP CONNECT");
            }
            String token = authorization.substring("Bearer ".length()).trim();
            try {
                Jwt jwt = jwtDecoder.decode(token);
                JwtAuthenticationToken authentication =
                        (JwtAuthenticationToken) Objects.requireNonNull(jwtAuthenticationConverter.convert(jwt));
                accessor.setUser(authentication);
                log.debug("WS CONNECT accepted for principal {}", authentication.getName());
            } catch (JwtException ex) {
                log.warn("WS CONNECT rejected: invalid JWT ({})", ex.getMessage());
                throw new BadCredentialsException("Invalid JWT on STOMP CONNECT", ex);
            }
        }
        return message;
    }
}
