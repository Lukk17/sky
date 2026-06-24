package com.lukk.sky.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Utility for extracting identity from the current request's JWT authentication.
 *
 * <p>The filter chain guarantees authentication on every protected endpoint, so
 * the {@code email} claim is always present when this method is called from a
 * controller method that is behind the security filter chain.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String currentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = (Jwt) jwtAuth.getPrincipal();
            String email = jwt.getClaimAsString("email");
            if (email == null || email.isBlank()) {
                throw new IllegalStateException(
                        "JWT does not contain an 'email' claim; check the Keycloak realm mapper configuration");
            }

            return email;
        }
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("email");
            if (email == null || email.isBlank()) {
                throw new IllegalStateException(
                        "JWT does not contain an 'email' claim; check the Keycloak realm mapper configuration");
            }

            return email;
        }
        throw new IllegalStateException(
                "No JWT authentication found in SecurityContext; ensure the resource server filter chain is applied");
    }
}
