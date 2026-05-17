package com.lukk.sky.notify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * HTTP-level security for sky-notify.
 *
 * <p>The actuator probes and the WebSocket handshake URL are public; STOMP CONNECT
 * frames are gated by {@code WebSocketAuthChannelInterceptor} (JWT validated against
 * the configured OAuth2 resource-server issuer). Everything else requires authn.
 *
 * <p>CSRF is disabled because the service exposes no state-changing HTTP endpoints;
 * the WebSocket auth is JWT-bearer and so not vulnerable to CSRF.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain notifySecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/notifyWebsocket/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> { /* defaults: validate via issuer-uri */ }))
                .build();
    }
}
