package com.lukk.sky.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * HTTP security for sky-booking.
 *
 * <p>Actuator probes and API/Swagger docs are public; all other requests require a valid
 * Bearer JWT. CSRF is disabled because this is a stateless JWT-authenticated API with no
 * session cookies.
 *
 * <p>The {@link JwtDecoder} bean is supplied either by
 * {@code ResourceServerJwtAutoConfiguration} (sky-common) in production or by
 * {@code TestSecurityConfig} in integration tests.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain bookingSecurityFilterChain(HttpSecurity http) throws Exception {
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
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> {
                }))
                .build();
    }
}
