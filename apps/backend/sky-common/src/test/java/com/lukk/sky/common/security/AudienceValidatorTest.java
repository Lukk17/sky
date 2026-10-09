package com.lukk.sky.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AudienceValidator")
class AudienceValidatorTest {

    private static final String EXPECTED_AUDIENCE = "sky-backend";

    private final AudienceValidator validator = new AudienceValidator(EXPECTED_AUDIENCE);

    @Test
    @DisplayName("validate_succeeds_whenAudienceClaimContainsTheExpectedAudience")
    void validate_succeeds_whenAudienceClaimContainsTheExpectedAudience() {
        // given
        Jwt jwt = jwtWithAudience(List.of("other", EXPECTED_AUDIENCE));

        // when
        OAuth2TokenValidatorResult result = validator.validate(jwt);

        // then
        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("validate_fails_whenAudienceClaimIsAbsent")
    void validate_fails_whenAudienceClaimIsAbsent() {
        // given
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user")
                .build();

        // when
        OAuth2TokenValidatorResult result = validator.validate(jwt);

        // then
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors().iterator().next().getDescription())
                .isEqualTo("JWT is missing required audience claim");
    }

    @Test
    @DisplayName("validate_fails_whenAudienceClaimIsEmpty")
    void validate_fails_whenAudienceClaimIsEmpty() {
        // given
        Jwt jwt = jwtWithAudience(List.of());

        // when
        OAuth2TokenValidatorResult result = validator.validate(jwt);

        // then
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors().iterator().next().getDescription())
                .as("an empty audience list must be rejected the same way a missing one is")
                .isEqualTo("JWT is missing required audience claim");
    }

    @Test
    @DisplayName("validate_fails_whenAudienceClaimHoldsADifferentAudience")
    void validate_fails_whenAudienceClaimHoldsADifferentAudience() {
        // given
        Jwt jwt = jwtWithAudience(List.of("someone-else"));

        // when
        OAuth2TokenValidatorResult result = validator.validate(jwt);

        // then
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors().iterator().next().getDescription())
                .isEqualTo("JWT audience claim does not contain the expected audience");
    }

    private Jwt jwtWithAudience(List<String> audiences) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user")
                .claim("aud", audiences)
                .build();
    }
}
