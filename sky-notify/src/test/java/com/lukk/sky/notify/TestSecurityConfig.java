package com.lukk.sky.notify;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Test-profile stub: provides a {@link JwtDecoder} bean so context-load smoke tests
 * succeed without a real Auth0 issuer reachable. Any actual STOMP CONNECT against this
 * decoder will fail loudly — auth-path behaviour belongs in
 * dedicated unit tests against {@code WebSocketAuthChannelInterceptor}, not in the
 * full @SpringBootTest context load.
 */
@TestConfiguration
@Profile("test")
public class TestSecurityConfig {

    @Bean
    @Primary
    public JwtDecoder testJwtDecoder() {
        return (token) -> {
            throw new JwtException("test-profile JwtDecoder: no real validation configured");
        };
    }
}
