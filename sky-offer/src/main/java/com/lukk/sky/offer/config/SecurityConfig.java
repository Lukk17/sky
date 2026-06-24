package com.lukk.sky.offer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * HTTP security for sky-offer.
 *
 * <p>Public (no token required):
 * <ul>
 *   <li>{@code GET /api/v1/offers} and {@code GET /api/v1/offers/**} — public offer listing</li>
 *   <li>{@code POST /api/v1/search} — offer search</li>
 *   <li>Actuator health/info probes</li>
 *   <li>Swagger / OpenAPI docs</li>
 * </ul>
 *
 * <p>Authenticated (valid Bearer JWT required):
 * <ul>
 *   <li>{@code /api/v1/owner/**} — owner CRUD on offers</li>
 *   <li>{@code /api/internal/**} — service-to-service owner lookup</li>
 *   <li>Everything else</li>
 * </ul>
 *
 * <p>CSRF is disabled because this is a stateless JWT-authenticated API with no session cookies.
 *
 * <p>The {@link org.springframework.security.oauth2.jwt.JwtDecoder} bean is supplied either
 * by {@code ResourceServerJwtAutoConfiguration} (sky-common) in production or by
 * {@code TestSecurityConfig} in integration tests.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain offerSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health/**",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/offers", "/api/v1/offers/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/search").permitAll()
                        .requestMatchers("/api/v1/owner/**").authenticated()
                        .requestMatchers("/api/internal/**").authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> {
                }))
                .build();
    }
}
