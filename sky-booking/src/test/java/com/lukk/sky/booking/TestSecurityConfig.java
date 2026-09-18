package com.lukk.sky.booking;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@TestConfiguration
@Profile("test")
public class TestSecurityConfig {

    @Bean
    @Primary
    public JwtDecoder testJwtDecoder() {
        return token -> {
            String email = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);

            return Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("email", email)
                    .claim("realm_access", Map.of("roles", List.of("user", "admin")))
                    .subject(email)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
        };
    }
}
