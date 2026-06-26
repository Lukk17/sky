package com.lukk.sky.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Reactive security for the gateway, selected by Spring profile.
 *
 * <p>Without an explicit {@link SecurityWebFilterChain}, Spring Boot's reactive security
 * auto-configuration secures every exchange (HTTP Basic plus default CSRF), so an unauthenticated
 * proxy request is rejected with 401, and a state-changing request with 403. The gateway is a
 * reverse proxy, not an authentication boundary in either mode, so each profile registers its own
 * chain explicitly rather than relying on the default.
 *
 * <p>Default profile (local development): all exchanges are permitted and CSRF is disabled. The
 * gateway forwards the inbound {@code Authorization} header untouched to the downstream services,
 * which validate the bearer token themselves as OAuth2 resource servers.
 *
 * <p>{@code secure} profile: every request must complete the Keycloak OIDC login flow and the
 * TokenRelay filter forwards the obtained bearer token downstream; actuator stays open for probes.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    @Profile("!secure")
    public SecurityWebFilterChain permitAllSecurityWebFilterChain(ServerHttpSecurity http) {
        http
            .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable);
        return http.build();
    }

    @Bean
    @Profile("secure")
    public SecurityWebFilterChain oidcSecurityWebFilterChain(ServerHttpSecurity http) {
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
