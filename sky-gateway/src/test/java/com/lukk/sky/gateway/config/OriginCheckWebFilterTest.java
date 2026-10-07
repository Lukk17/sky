package com.lukk.sky.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class OriginCheckWebFilterTest {

    private final OriginCheckWebFilter filter = new OriginCheckWebFilter("http://localhost:4200");

    private static MockServerWebExchange exchange(HttpMethod method, String origin, String referer) {
        var builder = MockServerHttpRequest.method(method, "http://gateway:5777/api/v1/offers")
                .header("Host", "gateway:5777");
        if (origin != null) {
            builder.header("Origin", origin);
        }
        if (referer != null) {
            builder.header("Referer", referer);
        }
        return MockServerWebExchange.from(builder);
    }

    private static WebFilterChain recordingChain(AtomicBoolean called) {
        return exchange -> {
            called.set(true);
            return Mono.empty();
        };
    }

    @Test
    @DisplayName("post_withForeignOrigin_thenForbiddenAndChainSkipped")
    void post_withForeignOrigin_thenForbiddenAndChainSkipped() {
        // given
        var exchange = exchange(HttpMethod.POST, "https://evil.test", null);
        var called = new AtomicBoolean(false);

        // when
        filter.filter(exchange, recordingChain(called)).block();

        // then
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(called).isFalse();
    }

    @Test
    @DisplayName("post_withMatchingOrigin_thenChainCalled")
    void post_withMatchingOrigin_thenChainCalled() {
        // given
        var exchange = exchange(HttpMethod.POST, "http://gateway:5777", null);
        var called = new AtomicBoolean(false);

        // when
        filter.filter(exchange, recordingChain(called)).block();

        // then
        assertThat(called).isTrue();
    }

    @Test
    @DisplayName("post_withFrontendOrigin_thenChainCalled")
    void post_withFrontendOrigin_thenChainCalled() {
        // given
        var exchange = exchange(HttpMethod.POST, "http://localhost:4200", null);
        var called = new AtomicBoolean(false);

        // when
        filter.filter(exchange, recordingChain(called)).block();

        // then
        assertThat(called).isTrue();
    }

    @Test
    @DisplayName("post_withForeignRefererAndNoOrigin_thenForbidden")
    void post_withForeignRefererAndNoOrigin_thenForbidden() {
        // given
        var exchange = exchange(HttpMethod.POST, null, "https://evil.test/page");
        var called = new AtomicBoolean(false);

        // when
        filter.filter(exchange, recordingChain(called)).block();

        // then
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(called).isFalse();
    }

    @Test
    @DisplayName("post_withNoOriginOrReferer_thenChainCalled")
    void post_withNoOriginOrReferer_thenChainCalled() {
        // given
        var exchange = exchange(HttpMethod.POST, null, null);
        var called = new AtomicBoolean(false);

        // when
        filter.filter(exchange, recordingChain(called)).block();

        // then
        assertThat(called).isTrue();
    }

    @Test
    @DisplayName("get_withForeignOrigin_thenChainCalled")
    void get_withForeignOrigin_thenChainCalled() {
        // given
        var exchange = exchange(HttpMethod.GET, "https://evil.test", null);
        var called = new AtomicBoolean(false);

        // when
        filter.filter(exchange, recordingChain(called)).block();

        // then
        assertThat(called).isTrue();
    }
}
