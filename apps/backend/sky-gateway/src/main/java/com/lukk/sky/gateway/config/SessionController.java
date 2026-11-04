package com.lukk.sky.gateway.config;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Reports the gateway session owner together with the session bound CSRF token.
 * Unauthenticated calls answer 401 through the security chain, so reaching this
 * method means a session exists. A bearer request skips the CSRF filter, so the
 * token is generated through the shared repository and persisted in the session
 * before it is returned. The response also carries the Keycloak end-session URL
 * (logoutUrl) so the browser can be routed through RP-initiated logout after the
 * gateway session is cleared; an XHR POST to /logout alone never shows the
 * Keycloak page.
 */
@RestController
public class SessionController {

    private final ServerCsrfTokenRepository csrfTokenRepository;

    @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri:}")
    private String issuerUri;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id:}")
    private String clientId;

    @Value("${sky-gateway.allowed-frontend-urls:http://localhost:4200}")
    private List<String> allowedFrontendUrls;

    public SessionController(ServerCsrfTokenRepository csrfTokenRepository) {
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @Operation(summary = "Get gateway session owner")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session owner"),
            @ApiResponse(responseCode = "401", description = "No session")
    })
    @GetMapping("/api/session")
    public Mono<Map<String, String>> session(Mono<Authentication> authentication, ServerWebExchange exchange) {
        Mono<CsrfToken> csrfToken = exchange.getAttributeOrDefault(CsrfToken.class.getName(), Mono.empty());

        return authentication
                .flatMap(auth -> Mono.justOrEmpty(GatewayIdentity.emailOf(auth)))
                .flatMap(email -> csrfTokenValue(csrfToken, exchange)
                        .map(token -> Map.of("email", email, "csrfToken", token, "logoutUrl", endSessionUrl())))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED)));
    }

    private Mono<String> csrfTokenValue(Mono<CsrfToken> csrfToken, ServerWebExchange exchange) {
        return tokenValue(csrfToken)
                .switchIfEmpty(Mono.defer(() -> tokenValue(csrfTokenRepository.loadToken(exchange))))
                .switchIfEmpty(Mono.defer(() -> generateToken(exchange)));
    }

    private static Mono<String> tokenValue(Mono<CsrfToken> csrfToken) {
        return csrfToken
                .mapNotNull(CsrfToken::getToken)
                .filter(token -> !token.isBlank());
    }

    private Mono<String> generateToken(ServerWebExchange exchange) {
        return csrfTokenRepository.generateToken(exchange)
                .flatMap(token -> csrfTokenRepository.saveToken(exchange, token)
                        .then(Mono.justOrEmpty(token.getToken())))
                .filter(token -> !token.isBlank());
    }

    private String endSessionUrl() {
        String issuer = issuerUri == null ? "" : issuerUri.trim().replaceAll("/+$", "");
        String origin = allowedFrontendUrl();
        String client = clientId == null ? "" : clientId.trim();
        return UriComponentsBuilder.fromUriString(issuer + "/protocol/openid-connect/logout")
                .queryParam("post_logout_redirect_uri", origin + "/home")
                .queryParam("client_id", client)
                .encode()
                .build()
                .toUriString();
    }

    private String allowedFrontendUrl() {
        String fallback = "http://localhost:4200";
        if (allowedFrontendUrls == null || allowedFrontendUrls.isEmpty()) {
            return fallback;
        }
        String candidate = allowedFrontendUrls.get(0) == null ? "" : allowedFrontendUrls.get(0).trim();
        if (candidate.isEmpty()) {
            return fallback;
        }
        String normalized = candidate.replaceAll("/+$", "");
        if (!allowedFrontendUrls.contains(candidate) && !allowedFrontendUrls.contains(normalized)) {
            return fallback;
        }
        return normalized;
    }
}
