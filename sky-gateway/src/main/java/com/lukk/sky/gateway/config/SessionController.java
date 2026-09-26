package com.lukk.sky.gateway.config;

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
 */
@RestController
public class SessionController {

    @GetMapping("/api/session")
    public Mono<Map<String, String>> session(Mono<Authentication> authentication, ServerWebExchange exchange) {
        Mono<CsrfToken> csrfToken = exchange.getAttribute(CsrfToken.class.getName());
        Mono<String> tokenValue = csrfToken != null
                ? csrfToken.map(CsrfToken::getToken)
                : Mono.empty();

        return authentication
                .map(auth -> emailOf(auth.getPrincipal()))
                .flatMap(email -> tokenValue
                        .map(token -> Map.of("email", email, "csrfToken", token))
                        .defaultIfEmpty(Map.of("email", email)))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED)));
    }

    private static String emailOf(Object principal) {
        if (principal instanceof OidcUser oidcUser && oidcUser.getEmail() != null) {
            return oidcUser.getEmail();
        }
        return String.valueOf(principal);
    }
}
