package com.lukk.sky.gateway.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

final class GatewayIdentity {

    private GatewayIdentity() {
    }

    static Optional<String> emailOf(Authentication authentication) {
        if (authentication == null) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            Optional<String> identity = fromJwt(jwtAuthentication.getToken());
            if (identity.isPresent()) {
                return identity;
            }
        } else if (principal instanceof OidcUser oidcUser) {
            Optional<String> email = nonBlank(oidcUser.getEmail());
            if (email.isPresent()) {
                return email;
            }
        } else if (authentication instanceof OAuth2AuthenticationToken oauth2) {
            Object email = oauth2.getPrincipal().getAttribute("email");
            Optional<String> value = nonBlank(email == null ? null : email.toString());
            if (value.isPresent()) {
                return value;
            }
        }
        return nonBlank(authentication.getName());
    }

    private static Optional<String> fromJwt(Jwt jwt) {
        if (jwt == null) {
            return Optional.empty();
        }
        Optional<String> email = nonBlank(jwt.getClaimAsString("email"));
        if (email.isPresent()) {
            return email;
        }
        return nonBlank(jwt.getSubject());
    }

    private static Optional<String> nonBlank(String value) {
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }
}
