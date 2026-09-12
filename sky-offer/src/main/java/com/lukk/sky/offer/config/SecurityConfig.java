package com.lukk.sky.offer.config;

import com.lukk.sky.common.security.SecurityPaths;
import com.lukk.sky.common.security.SkySecurityDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain offerSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        return SkySecurityDefaults.filterChain(http, jwtAuthenticationConverter, SecurityPaths.probesAndApiDocs(), auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/v1/offers/*/owner").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/offers", "/api/v1/offers/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/search").permitAll()
                .requestMatchers("/api/v1/owner/**").authenticated());
    }
}
