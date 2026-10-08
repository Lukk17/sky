package com.lukk.sky.notify.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.core.Ordered;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor, Ordered {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                acceptBearer(accessor, authorization);
                return message;
            }
            Object gatewayUser = accessor.getSessionAttributes() == null
                    ? null
                    : accessor.getSessionAttributes().get(GatewayUserHandshakeInterceptor.GATEWAY_USER_ATTRIBUTE);
            if (gatewayUser instanceof String name && !name.isBlank()) {
                accessor.setUser(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        name, "N/A", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))));
                log.debug("WS CONNECT accepted for gateway session user {}", name);
                return message;
            }
            log.warn("WS CONNECT rejected: missing or malformed Authorization header");
            throw new BadCredentialsException("Missing Bearer token on STOMP CONNECT");
        }
        return message;
    }

    private void acceptBearer(StompHeaderAccessor accessor, String authorization) {
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
}
