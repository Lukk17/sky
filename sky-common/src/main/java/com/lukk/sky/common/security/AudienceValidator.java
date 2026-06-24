package com.lukk.sky.common.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Validates that a JWT's {@code aud} claim contains the expected audience value.
 *
 * <p>Instantiate only when a non-blank audience is configured; omitting the
 * validator when no audience is expected avoids breaking existing deployments
 * that do not set {@code OAUTH2_AUDIENCE}.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final String AUDIENCE_CLAIM = "aud";
    private static final OAuth2Error MISSING_AUD_ERROR = new OAuth2Error(
            "invalid_token",
            "JWT is missing required audience claim",
            null);
    private static final OAuth2Error WRONG_AUD_ERROR = new OAuth2Error(
            "invalid_token",
            "JWT audience claim does not contain the expected audience",
            null);

    private final String expectedAudience;

    public AudienceValidator(String expectedAudience) {
        this.expectedAudience = expectedAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> audiences = jwt.getClaimAsStringList(AUDIENCE_CLAIM);
        if (audiences == null || audiences.isEmpty()) {
            return OAuth2TokenValidatorResult.failure(MISSING_AUD_ERROR);
        }
        if (!audiences.contains(expectedAudience)) {
            return OAuth2TokenValidatorResult.failure(WRONG_AUD_ERROR);
        }

        return OAuth2TokenValidatorResult.success();
    }
}
