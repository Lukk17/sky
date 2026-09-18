package com.lukk.sky.common.security;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityUtils {

    private static final String EMAIL_CLAIM = "email";

    /**
     * Returns the {@code email} claim of the JWT authenticating the current request.
     *
     * @throws IllegalStateException when the request carries no JWT, or the JWT has no {@code email} claim
     */
    public static String currentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return emailClaimOf(jwtAuthentication.getToken());
        }
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return emailClaimOf(jwt);
        }

        throw new IllegalStateException(
                "No JWT authentication found in SecurityContext; ensure the resource server filter chain is applied");
    }

    private static String emailClaimOf(Jwt jwt) {
        String email = jwt.getClaimAsString(EMAIL_CLAIM);
        if (!StringUtils.hasText(email)) {
            throw new IllegalStateException(
                    "JWT does not contain an 'email' claim; check the Keycloak realm mapper configuration");
        }

        return email;
    }
}
