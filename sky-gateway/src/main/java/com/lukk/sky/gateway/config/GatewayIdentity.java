package com.lukk.sky.gateway.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

final class GatewayIdentity {

    private GatewayIdentity() {
    }

    static String emailOf(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser && oidcUser.getEmail() != null && !oidcUser.getEmail().isBlank()) {
            return oidcUser.getEmail();
        }
        if (authentication instanceof OAuth2AuthenticationToken oauth2) {
            Object email = oauth2.getPrincipal().getAttribute("email");
            if (email != null && !email.toString().isBlank()) {
                return email.toString();
            }
        }
        return authentication.getName();
    }
}
