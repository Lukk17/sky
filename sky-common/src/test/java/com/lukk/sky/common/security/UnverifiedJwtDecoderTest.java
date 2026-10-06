package com.lukk.sky.common.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UnverifiedJwtDecoder")
class UnverifiedJwtDecoderTest {

    private static final String SIGNING_SECRET = "local-profile-signing-secret-that-is-long-enough";

    private final UnverifiedJwtDecoder decoder = new UnverifiedJwtDecoder();

    @Test
    @DisplayName("decode_readsClaims_whenTheSigningKeyIsUnknown")
    void decode_readsClaims_whenTheSigningKeyIsUnknown() throws Exception {
        // given
        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String token = signedToken(new JWTClaimsSet.Builder()
                .subject("local-user")
                .issuer("https://keycloak.test/realms/sky")
                .claim("email", "local@example.com")
                .claim("realm_access", Map.of("roles", List.of("user")))
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(issuedAt.plus(1, ChronoUnit.HOURS)))
                .build());

        // when
        Jwt jwt = decoder.decode(token);

        // then
        assertThat(jwt.getSubject()).isEqualTo("local-user");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("local@example.com");
        assertThat(jwt.getIssuedAt())
                .as("timestamp claims must be converted to Instant so Jwt can be built")
                .isEqualTo(issuedAt);
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
    }

    @Test
    @DisplayName("decode_acceptsAnExpiredToken_becauseTheLocalProfileValidatesNothing")
    void decode_acceptsAnExpiredToken_becauseTheLocalProfileValidatesNothing() throws Exception {
        // given
        Instant expiredAt = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        String token = signedToken(new JWTClaimsSet.Builder()
                .subject("local-user")
                .expirationTime(Date.from(expiredAt))
                .build());

        // when
        Jwt jwt = decoder.decode(token);

        // then
        assertThat(jwt.getExpiresAt()).isEqualTo(expiredAt);
    }

    @Test
    @DisplayName("decode_throwsBadJwt_whenTheTokenIsNotAJwt")
    void decode_throwsBadJwt_whenTheTokenIsNotAJwt() {
        assertThatThrownBy(() -> decoder.decode("not-a-token"))
                .isInstanceOf(BadJwtException.class)
                .hasMessageContaining("well-formed");
    }

    @Test
    @DisplayName("decode_throwsBadJwt_whenTheTokenIsBlank")
    void decode_throwsBadJwt_whenTheTokenIsBlank() {
        assertThatThrownBy(() -> decoder.decode("  "))
                .isInstanceOf(BadJwtException.class)
                .hasMessageContaining("no bearer token");
    }

    private String signedToken(JWTClaimsSet claims) throws Exception {
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        signedJWT.sign(new MACSigner(SIGNING_SECRET));

        return signedJWT.serialize();
    }
}
