package com.lukk.sky.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("SkySecurityDefaults without additional rules")
class SkySecurityDefaultsWithoutAdditionalRulesTest {

    private SecurityFilterChainFixture fixture;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        fixture = new SecurityFilterChainFixture(ProbesOnlySecurityConfig.class);
        mvc = fixture.mvc();
    }

    @AfterEach
    void tearDown() {
        fixture.close();
    }

    @Test
    @DisplayName("healthProbeIsOpen_withoutAToken")
    void healthProbeIsOpen_withoutAToken() throws Exception {
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("apiDocsAreClosed_whenTheServicePassedProbesOnly")
    void apiDocsAreClosed_whenTheServicePassedProbesOnly() throws Exception {
        mvc.perform(get("/v3/api-docs/public")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("businessEndpointIsClosed_withoutAToken")
    void businessEndpointIsClosed_withoutAToken() throws Exception {
        mvc.perform(get("/api/v1/offers/42")).andExpect(status().isUnauthorized());
    }

    @Configuration
    @EnableWebSecurity
    static class ProbesOnlySecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
            converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());

            return SkySecurityDefaults.filterChain(http, converter, SecurityPaths.probes());
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return new UnverifiedJwtDecoder();
        }
    }
}
