package com.lukk.sky.notify.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import com.lukk.sky.common.security.AudienceValidator;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

/**
 * HTTP-level security for sky-notify.
 *
 * <p>The actuator probes and the WebSocket handshake URL are public; STOMP CONNECT
 * frames are gated by {@code WebSocketAuthChannelInterceptor} (JWT validated against
 * the configured OAuth2 resource-server issuer). Everything else requires authn.
 *
 * <p>CSRF is disabled because the service exposes no state-changing HTTP endpoints;
 * the WebSocket auth is JWT-bearer and so not vulnerable to CSRF.
 *
 * <p>The {@link JwtDecoder} bean validates signature, expiry, and issuer by default.
 * When {@code OAUTH2_AUDIENCE} is set it additionally requires the {@code aud} claim
 * to contain the configured value, preventing token reuse across services.
 *
 * <p>{@code jwtDecoder()} carries {@link ConditionalOnMissingBean} so that test
 * profiles can supply a stub decoder (see {@code TestSecurityConfig}) without
 * triggering the OIDC discovery HTTP call that {@link NimbusJwtDecoder#withIssuerLocation}
 * makes at build time.
 */
@Configuration
public class SecurityConfig {

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${OAUTH2_AUDIENCE:}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(JwtValidators.createDefaultWithIssuer(issuerUri));

        if (audience != null && !audience.isBlank()) {
            validators.add(new AudienceValidator(audience));
        }

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));

        return decoder;
    }

    @Bean
    public SecurityFilterChain notifySecurityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder)
            throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/notifyWebsocket/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.decoder(jwtDecoder)))
                .build();
    }
}
