package com.lukk.sky.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.DefaultCsrfToken;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionControllerTest {

    private final SessionController controller = new SessionController();

    private static OidcIdToken idToken(String email) {
        return new OidcIdToken(
                "id-token-value",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("sub", "user-1", "email", email));
    }

    private static Authentication authentication(Object principal) {
        return new TestingAuthenticationToken(principal, null);
    }

    private static MockServerWebExchange exchangeWithCsrfToken() {
        var exchange = MockServerWebExchange.from(
                org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/api/session"));
        exchange.getAttributes().put(CsrfToken.class.getName(),
                Mono.just(new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "csrf-token-value")));
        return exchange;
    }

    @Test
    void session_withOidcUser_thenReturnsEmailAndCsrfToken() {
        var user = new DefaultOidcUser(List.of(), idToken("user@sky.dev"));

        assertThat(controller
                .session(Mono.just(authentication(user)), exchangeWithCsrfToken())
                .block())
                .containsEntry("email", "user@sky.dev")
                .containsEntry("csrfToken", "csrf-token-value")
                .containsKey("logoutUrl");
    }

    @Test
    void session_withPlainPrincipal_thenReturnsName() {
        assertThat(controller
                .session(Mono.just(authentication("plain-user")), MockServerWebExchange.from(
                        org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/api/session")))
                .block())
                .containsEntry("email", "plain-user")
                .containsKey("logoutUrl");
    }

    @Test
    void session_withNoPrincipal_thenUnauthorized() {
        assertThatThrownBy(() -> controller
                .session(Mono.empty(), MockServerWebExchange.from(
                        org.springframework.mock.http.server.reactive.MockServerHttpRequest.get("/api/session")))
                .block())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }
}
