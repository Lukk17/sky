package com.lukk.sky.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GatewayUserWebFilter: strip plus set identity on notify upgrade")
class GatewayUserWebFilterTest {

    private final GatewayUserWebFilter filter = new GatewayUserWebFilter();

    @Test
    @DisplayName("strips client-set header and sets session principal name")
    void filter_whenNotifyPathWithSession_thenStripsAndSets() {
        // given
        // when
        // then
        var request = MockServerHttpRequest.get("/notifyWebsocket")
                .header(GatewayUserWebFilter.GATEWAY_USER_HEADER, "attacker@evil.test")
                .build();
        var exchange = MockServerWebExchange.from(request);
        var auth = new UsernamePasswordAuthenticationToken("user@test.com", "N/A", java.util.List.of());
        var ctx = new SecurityContextImpl(auth);
        String[] seen = new String[1];
        GatewayFilterChain chain = e -> {
            seen[0] = e.getRequest().getHeaders().getFirst(GatewayUserWebFilter.GATEWAY_USER_HEADER);
            return Mono.empty();
        };

        filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(ctx)))
                .block();

        assertThat(seen[0]).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("leaves non-notify paths untouched including client header")
    void filter_whenOtherPath_thenUntouched() {
        // given
        // when
        // then
        var request = MockServerHttpRequest.get("/api/v1/offers")
                .header(GatewayUserWebFilter.GATEWAY_USER_HEADER, "attacker@evil.test")
                .build();
        var exchange = MockServerWebExchange.from(request);
        String[] seen = new String[1];
        GatewayFilterChain chain = e -> {
            seen[0] = e.getRequest().getHeaders().getFirst(GatewayUserWebFilter.GATEWAY_USER_HEADER);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(seen[0]).isEqualTo("attacker@evil.test");
    }
}
