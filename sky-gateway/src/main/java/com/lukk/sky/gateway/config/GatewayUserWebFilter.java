package com.lukk.sky.gateway.config;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class GatewayUserWebFilter implements GlobalFilter, Ordered {

    public static final String GATEWAY_USER_HEADER = "X-Sky-User";

    @Override
    public int getOrder() {
        return -1000;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!path.startsWith("/notifyWebsocket")) {
            return chain.filter(exchange);
        }
        ServerHttpRequest stripped = exchange.getRequest().mutate()
                .headers(headers -> headers.remove(GATEWAY_USER_HEADER))
                .build();
        ServerWebExchange strippedExchange = exchange.mutate().request(stripped).build();
        return ReactiveSecurityContextHolder.getContext()
                .mapNotNull(ctx -> ctx.getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(GatewayUserWebFilter::principalName)
                .doOnNext(name -> log.debug("gateway WS identity: forwarding notify upgrade as {}", name))
                .flatMap(name -> {
                    ServerHttpRequest withUser = strippedExchange.getRequest().mutate()
                            .headers(headers -> headers.set(GATEWAY_USER_HEADER, name))
                            .build();
                    return chain.filter(strippedExchange.mutate().request(withUser).build());
                })
                .switchIfEmpty(chain.filter(strippedExchange));
    }

    static String principalName(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser oidcUser) {
            String email = oidcUser.getEmail();
            if (email != null && !email.isBlank()) {
                return email;
            }
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
