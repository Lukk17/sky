package com.lukk.sky.gateway.config;

import com.lukk.sky.common.web.CorrelationId;
import java.util.UUID;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    String incoming = exchange.getRequest().getHeaders().getFirst(CorrelationId.HEADER);
    String correlationId = StringUtils.hasText(incoming) ? incoming : UUID.randomUUID().toString();

    exchange.getResponse().getHeaders().set(CorrelationId.HEADER, correlationId);
    ServerHttpRequest mutatedRequest =
        exchange.getRequest().mutate().headers(headers -> headers.set(CorrelationId.HEADER, correlationId)).build();
    ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

    CorrelationId.set(correlationId);
    return chain.filter(mutatedExchange).doFinally(signal -> CorrelationId.clear());
  }
}
