package com.lukk.sky.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("SkySecurityDefaults")
class SkySecurityDefaultsTest {

    private SecurityFilterChainFixture fixture;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        fixture = new SecurityFilterChainFixture(ServiceSecurityConfig.class);
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
    @DisplayName("apiDocsAreOpen_withoutAToken")
    void apiDocsAreOpen_withoutAToken() throws Exception {
        mvc.perform(get("/v3/api-docs/public")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("prometheusIsClosed_withoutAToken")
    void prometheusIsClosed_withoutAToken() throws Exception {
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("businessEndpointIsClosed_withoutAToken")
    void businessEndpointIsClosed_withoutAToken() throws Exception {
        mvc.perform(get("/api/v1/bookings")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("unauthorizedResponseHasAnEmptyBodyAndABearerChallenge_soTheOpenApiContractDeclaresNoContent")
    void unauthorizedResponseHasAnEmptyBodyAndABearerChallenge() throws Exception {
        mvc.perform(get("/api/v1/bookings"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("serviceSpecificRuleIsApplied_beforeTheAuthenticatedFallback")
    void serviceSpecificRuleIsApplied_beforeTheAuthenticatedFallback() throws Exception {
        mvc.perform(get("/api/v1/offers/42")).andExpect(status().isNotFound());
    }

    @Configuration
    @EnableWebSecurity
    static class ServiceSecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
            converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());

            return SkySecurityDefaults.filterChain(
                    http,
                    converter,
                    SecurityPaths.probesAndApiDocs(),
                    auth -> auth.requestMatchers(HttpMethod.GET, "/api/v1/offers/**").permitAll());
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return new UnverifiedJwtDecoder();
        }
    }
}
