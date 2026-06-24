package com.lukk.sky.message;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Test-profile stub: the token is the base64url-encoded user email, decoded into the email claim.
 *
 * <p>Integration tests call {@code headers.setBearerAuth(base64Url(email))} and the stub decoder
 * decodes it back into a {@link Jwt} with {@code email}. The email is base64url-encoded because a
 * raw email contains '@', which is outside the RFC 6750 Bearer-token charset and would be rejected
 * as a malformed token before the decoder runs. This lets {@code SecurityUtils.currentUserEmail()}
 * return the correct identity without a real Keycloak server.
 *
 * <p>{@code @Primary} wins over the {@code @ConditionalOnMissingBean}-guarded bean in
 * {@code ResourceServerJwtAutoConfiguration}, so no OIDC discovery HTTP call is made during tests.
 */
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
                    .subject(email)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
        };
    }
}
