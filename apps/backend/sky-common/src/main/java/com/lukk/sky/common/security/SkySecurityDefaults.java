package com.lukk.sky.common.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SkySecurityDefaults {

    public static SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            List<String> publicPaths) throws Exception {

        return filterChain(http, jwtAuthenticationConverter, publicPaths, auth -> {
        });
    }

    /**
     * Builds the chain every sky service shares: CSRF off, {@code publicPaths} open, JWT resource server on,
     * {@code additionalRules} applied in order, and every remaining request authenticated.
     *
     * @param additionalRules service-specific rules, always evaluated after {@code publicPaths}
     */
    public static SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            List<String> publicPaths,
            Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry>
                    additionalRules) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(publicPaths.toArray(String[]::new)).permitAll();
                    additionalRules.customize(auth);
                    auth.anyRequest().authenticated();
                })
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }
}
