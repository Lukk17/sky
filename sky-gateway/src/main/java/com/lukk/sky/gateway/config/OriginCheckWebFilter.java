package com.lukk.sky.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Set;

/**
 * Rejects cross origin state changing requests whose {@code Origin} or {@code Referer}
 * names an origin (scheme plus host plus port) other than the request origin.
 * Requests carrying neither header are let through, so non browser callers
 * without an origin concept keep working.
 */
@Component
@Profile("!local")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class OriginCheckWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(OriginCheckWebFilter.class);

    private static final Set<HttpMethod> MUTATING =
            Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE);

    private final String frontendOrigin;

    public OriginCheckWebFilter(
            @Value("${sky-gateway.frontend-url:http://localhost:4200}")
            String frontendUrl) {
        this.frontendOrigin = SecurityConfig.validatedFrontendUrl(frontendUrl).toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        // Keycloak and the object store answer from behind /auth and /s3 with their
        // own forgery protection (OIDC state and nonce, presigned capability URLs),
        // so the gateway session origin check must not gate their browser posts:
        // privacy-hardened browsers legitimately send an opaque (null) origin there.
        if (path.startsWith("/auth/") || path.startsWith("/s3/")) {
            return chain.filter(exchange);
        }
        if (!MUTATING.contains(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        var hostHeader = exchange.getRequest().getHeaders().getHost();
        if (hostHeader == null) {
            return reject(exchange, "missing-host");
        }
        String expected = requestOrigin(exchange);
        String origin = exchange.getRequest().getHeaders().getOrigin();

        if (origin != null && !allowed(origin, expected)) {
            return reject(exchange, "origin-mismatch");
        }

        String referer = exchange.getRequest().getHeaders().getFirst("Referer");
        if (origin == null && referer != null && !allowed(referer, expected)) {
            return reject(exchange, "referer-mismatch");
        }

        return chain.filter(exchange);
    }

    private boolean allowed(String candidate, String requestOrigin) {
        String normalized = normalizeOrigin(candidate);
        return requestOrigin.equalsIgnoreCase(normalized) || frontendOrigin.equalsIgnoreCase(normalized);
    }

    private static Mono<Void> reject(ServerWebExchange exchange, String reason) {
        log.warn("origin check rejected {} {} reason={}", exchange.getRequest().getMethod(),
                exchange.getRequest().getPath().value(), reason);
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    private static String requestOrigin(ServerWebExchange exchange) {
        var uri = exchange.getRequest().getURI();
        String scheme = uri.getScheme() == null ? "http" : uri.getScheme().toLowerCase(java.util.Locale.ROOT);
        var hostHeader = exchange.getRequest().getHeaders().getHost();
        String host = hostHeader != null ? hostHeader.getHostString().toLowerCase(java.util.Locale.ROOT)
                : (uri.getHost() == null ? "" : uri.getHost().toLowerCase(java.util.Locale.ROOT));
        int port = hostHeader != null ? hostHeader.getPort() : uri.getPort();
        return normalize(scheme, host, port);
    }

    private static String normalizeOrigin(String uri) {
        try {
            URI parsed = URI.create(uri);
            String scheme = parsed.getScheme() == null ? "" : parsed.getScheme().toLowerCase(java.util.Locale.ROOT);
            String host = parsed.getHost() == null ? "" : parsed.getHost().toLowerCase(java.util.Locale.ROOT);
            return normalize(scheme, host, parsed.getPort());
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private static String normalize(String scheme, String host, int port) {
        return scheme + "://" + host + (port == -1 ? "" : ":" + port);
    }
}
