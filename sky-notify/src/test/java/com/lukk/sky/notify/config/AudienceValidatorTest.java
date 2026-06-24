package com.lukk.sky.notify.config;

import com.lukk.sky.common.security.AudienceValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AudienceValidator — JWT audience claim validation")
class AudienceValidatorTest {

    private static final String EXPECTED_AUDIENCE = "sky-notify-api";

    private final AudienceValidator validator = new AudienceValidator(EXPECTED_AUDIENCE);

    @Test
    @DisplayName("passes when aud claim contains the expected audience")
    void validate_whenAudContainsExpectedValue_thenSuccess() {
        Jwt jwt = buildJwt(List.of(EXPECTED_AUDIENCE, "other-service"));

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("passes when aud claim contains only the expected audience")
    void validate_whenAudContainsExactlyExpectedValue_thenSuccess() {
        Jwt jwt = buildJwt(List.of(EXPECTED_AUDIENCE));

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("fails when aud claim is absent")
    void validate_whenAudClaimAbsent_thenFailure() {
        Jwt jwt = buildJwtWithoutAud();

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
                .anyMatch(e -> e.getDescription().contains("missing required audience claim"));
    }

    @Test
    @DisplayName("fails when aud claim is present but does not contain the expected audience")
    void validate_whenAudClaimDoesNotContainExpectedValue_thenFailure() {
        Jwt jwt = buildJwt(List.of("some-other-service", "another-api"));

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors())
                .anyMatch(e -> e.getDescription().contains("does not contain the expected audience"));
    }

    @Test
    @DisplayName("fails when aud claim is an empty list")
    void validate_whenAudClaimIsEmpty_thenFailure() {
        Jwt jwt = buildJwt(List.of());

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
    }

    private Jwt buildJwt(List<String> audiences) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .issuer("https://auth.example.com/realms/sky")
                .subject("user-123")
                .audience(audiences)
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
    }

    private Jwt buildJwtWithoutAud() {
        return Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .claims(claims -> {
                    claims.put("iss", "https://auth.example.com/realms/sky");
                    claims.put("sub", "user-123");
                    claims.put("iat", Instant.now().minusSeconds(60));
                    claims.put("exp", Instant.now().plusSeconds(300));
                })
                .build();
    }
}
