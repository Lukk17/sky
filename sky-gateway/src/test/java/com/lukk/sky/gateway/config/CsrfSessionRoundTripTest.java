package com.lukk.sky.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.mock.web.server.MockWebSession;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.CsrfWebFilter;
import org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CsrfSessionRoundTripTest {

    private final WebSessionServerCsrfTokenRepository repository = csrfTokenRepository();
    private final CsrfWebFilter filter = new CsrfWebFilter();

    CsrfSessionRoundTripTest() {
        filter.setCsrfTokenRepository(repository);
    }

    private static WebSessionServerCsrfTokenRepository csrfTokenRepository() {
        var repository = new WebSessionServerCsrfTokenRepository();
        repository.setHeaderName("X-XSRF-TOKEN");
        return repository;
    }

    @Test
    void tokenIssuedOnGet_thenValidatesOnPost() {
        var session = new MockWebSession();
        var get = MockServerWebExchange.builder(MockServerHttpRequest.get("/api/session").build())
                .session(session)
                .build();

        AtomicReference<String> exposed = new AtomicReference<>();
        filter.filter(get, chain -> {
            Mono<CsrfToken> attribute = get.getAttribute(CsrfToken.class.getName());
            assertThat(attribute).isNotNull();
            return attribute.doOnNext(token -> exposed.set(token.getToken())).then();
        }).block();

        assertThat(exposed.get()).isNotNull();

        var post = MockServerWebExchange.builder(MockServerHttpRequest
                        .method(HttpMethod.POST, "/logout")
                        .header("X-XSRF-TOKEN", exposed.get())
                        .build())
                .session(session)
                .build();

        var status = new AtomicReference<HttpStatus>();
        filter.filter(post, chain -> {
            status.set(HttpStatus.OK);
            return Mono.empty();
        }).block();

        assertThat(status.get()).isEqualTo(HttpStatus.OK);
    }
}
