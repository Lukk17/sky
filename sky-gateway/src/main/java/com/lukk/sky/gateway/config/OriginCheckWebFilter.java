package com.lukk.sky.gateway.config;

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
 * names a host other than the request host. Requests carrying neither header are let
 * through, so non browser callers without an origin concept keep working.
 */
@Component
@Profile("!local")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class OriginCheckWebFilter implements WebFilter {

    private static final Set<HttpMethod> MUTATING =
            Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!MUTATING.contains(exchange.getRequest().getMethod())) {
            return chain.filter(exchange);
        }

        var hostHeader = exchange.getRequest().getHeaders().getHost();
        if (hostHeader == null) {
            return reject(exchange);
        }
        String host = hostHeader.getHostString();
        String origin = exchange.getRequest().getHeaders().getOrigin();

        if (origin != null && !host.equalsIgnoreCase(hostOf(origin))) {
            return reject(exchange);
        }

        String referer = exchange.getRequest().getHeaders().getFirst("Referer");
        if (origin == null && referer != null && !host.equalsIgnoreCase(hostOf(referer))) {
            return reject(exchange);
        }

        return chain.filter(exchange);
    }

    private static Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
    }

    private static String hostOf(String uri) {
        try {
            return URI.create(uri).getHost();
        } catch (IllegalArgumentException e) {
            return "";
        }
    }
}
