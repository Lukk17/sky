package com.lukk.sky.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security configuration activated only under the {@code secure} Spring profile.
 *
 * When the {@code secure} profile is inactive (the default for local development),
 * no SecurityWebFilterChain bean is registered and Spring Security's auto-configuration
 * permits all traffic — developers can call the gateway without a running Keycloak instance.
 *
 * When the {@code secure} profile is active, every request must be authenticated via the
 * Keycloak OIDC login flow and the TokenRelay filter forwards the bearer token downstream.
 */
@Configuration
@EnableWebFluxSecurity
@Profile("secure")
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/**").permitAll()
                .anyExchange().authenticated()
            )
            .oauth2Login(login -> {})
            .csrf(ServerHttpSecurity.CsrfSpec::disable);
        return http.build();
    }
}
