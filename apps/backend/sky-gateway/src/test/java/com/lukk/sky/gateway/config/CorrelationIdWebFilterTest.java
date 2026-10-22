package com.lukk.sky.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.lukk.sky.common.web.CorrelationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@DisplayName("CorrelationIdWebFilter: mint plus forward X-Correlation-Id")
class CorrelationIdWebFilterTest {

  private final CorrelationIdWebFilter filter = new CorrelationIdWebFilter();

  @Test
  @DisplayName("forwards incoming header downstream and back on the response")
  void filter_whenHeaderPresent_thenForwardsSameValue() {
    var request = MockServerHttpRequest.get("/api/v1/offers").header(CorrelationId.HEADER, "abc-123").build();
    var exchange = MockServerWebExchange.from(request);
    String[] downstream = new String[1];
    WebFilterChain chain =
        e -> {
          downstream[0] = e.getRequest().getHeaders().getFirst(CorrelationId.HEADER);
          return Mono.empty();
        };

    filter.filter(exchange, chain).block();

    assertThat(downstream[0]).isEqualTo("abc-123");
    assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationId.HEADER)).isEqualTo("abc-123");
  }

  @Test
  @DisplayName("mints a header when absent and forwards it downstream")
  void filter_whenHeaderAbsent_thenMintsAndForwards() {
    var request = MockServerHttpRequest.get("/api/v1/offers").build();
    var exchange = MockServerWebExchange.from(request);
    String[] downstream = new String[1];
    WebFilterChain chain =
        e -> {
          downstream[0] = e.getRequest().getHeaders().getFirst(CorrelationId.HEADER);
          return Mono.empty();
        };

    filter.filter(exchange, chain).block();

    assertThat(downstream[0]).isNotBlank();
    assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationId.HEADER)).isEqualTo(downstream[0]);
  }
}
