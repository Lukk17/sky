package com.lukk.sky.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Reports the gateway session owner together with the session bound CSRF token.
 * Unauthenticated calls answer 401 through the security chain, so reaching this
 * method means a session exists. Reading the token here materializes it, which is
 * what persists it in the session for later validation.
 * The response also carries the Keycloak end-session URL (logoutUrl) so the
 * browser can be routed through RP-initiated logout after the gateway session
 * is cleared; an XHR POST to /logout alone never shows the Keycloak page.
 */
@RestController
public class SessionController {

    @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri:}")
    private String issuerUri;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id:}")
    private String clientId;

    @Value("${sky-gateway.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @GetMapping("/api/session")
    public Mono<Map<String, String>> session(Mono<Authentication> authentication, ServerWebExchange exchange) {
        Mono<CsrfToken> csrfToken = exchange.getAttribute(CsrfToken.class.getName());
        Mono<String> tokenValue = csrfToken != null
                ? csrfToken.map(CsrfToken::getToken)
                : Mono.empty();

        return authentication
                .map(auth -> emailOf(auth.getPrincipal()))
                .flatMap(email -> tokenValue
                        .map(token -> Map.of("email", email, "csrfToken", token, "logoutUrl", endSessionUrl()))
                        .defaultIfEmpty(Map.of("email", email, "logoutUrl", endSessionUrl())))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED)));
    }

    private String endSessionUrl() {
        String issuer = issuerUri == null ? "" : issuerUri.trim().replaceAll("/+$", "");
        String origin = frontendUrl == null || frontendUrl.isBlank()
                ? "http://localhost:4200"
                : frontendUrl.trim().replaceAll("/+$", "");
        String client = clientId == null ? "" : clientId.trim();
        return issuer + "/protocol/openid-connect/logout"
                + "?post_logout_redirect_uri=" + origin + "/home"
                + "&client_id=" + client;
    }

    private static String emailOf(Object principal) {
        if (principal instanceof OidcUser oidcUser && oidcUser.getEmail() != null) {
            return oidcUser.getEmail();
        }
        return String.valueOf(principal);
    }
}
