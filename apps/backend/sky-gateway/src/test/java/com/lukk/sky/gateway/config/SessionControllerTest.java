package com.lukk.sky.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.mock.web.server.MockWebSession;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.server.csrf.CsrfToken;
import org.springframework.security.web.server.csrf.DefaultCsrfToken;
import org.springframework.security.web.server.csrf.ServerCsrfTokenRepository;
import org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionControllerTest {

    private final ServerCsrfTokenRepository csrfTokenRepository = csrfTokenRepository();
    private final SessionController controller = new SessionController(csrfTokenRepository);

    private static ServerCsrfTokenRepository csrfTokenRepository() {
        var repository = new WebSessionServerCsrfTokenRepository();
        repository.setHeaderName("X-XSRF-TOKEN");
        return repository;
    }

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

    private static JwtAuthenticationToken bearerToken(String subject, String email) {
        var builder = Jwt.withTokenValue("bearer-token")
                .header("alg", "none")
                .claim("iss", "sky-test");
        if (subject != null) {
            builder.subject(subject);
        }
        if (email != null) {
            builder.claim("email", email);
        }
        return new JwtAuthenticationToken(builder.build(), List.of());
    }

    private static MockServerWebExchange exchange(MockWebSession session) {
        return MockServerWebExchange.builder(
                        MockServerHttpRequest.get("/api/session").build())
                .session(session)
                .build();
    }

    private static MockServerWebExchange exchangeWithCsrfToken(MockWebSession session) {
        var exchange = exchange(session);
        exchange.getAttributes().put(CsrfToken.class.getName(),
                Mono.just(new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "csrf-token-value")));
        return exchange;
    }

    @Test
    @DisplayName("session_withBearerJwt_thenReturnsEmailAndGeneratedCsrfToken")
    void session_withBearerJwt_thenReturnsEmailAndGeneratedCsrfToken() {
        // given
        var exchange = exchange(new MockWebSession());

        // when
        Map<String, String> body = controller
                .session(Mono.just(bearerToken("bearer-user", "bearer@sky.dev")), exchange)
                .block();

        // then
        assertThat(body)
                .containsEntry("email", "bearer@sky.dev")
                .containsKey("logoutUrl");
        assertThat(body.get("csrfToken")).isNotBlank();
        assertThat(csrfTokenRepository.loadToken(exchange).block())
                .isNotNull()
                .extracting(CsrfToken::getToken)
                .isEqualTo(body.get("csrfToken"));
    }

    @Test
    @DisplayName("session_withBearerJwtWithoutEmailClaim_thenReturnsSubject")
    void session_withBearerJwtWithoutEmailClaim_thenReturnsSubject() {
        // when
        Map<String, String> body = controller
                .session(Mono.just(bearerToken("bearer-subject", null)), exchange(new MockWebSession()))
                .block();

        // then
        assertThat(body).containsEntry("email", "bearer-subject");
        assertThat(body.get("csrfToken")).isNotBlank();
    }

    @Test
    @DisplayName("session_withOidcUser_thenReturnsEmailAndCsrfToken")
    void session_withOidcUser_thenReturnsEmailAndCsrfToken() {
        // given
        var user = new DefaultOidcUser(List.of(), idToken("user@sky.dev"));

        // when / then
        assertThat(controller
                .session(Mono.just(authentication(user)), exchangeWithCsrfToken(new MockWebSession()))
                .block())
                .containsEntry("email", "user@sky.dev")
                .containsEntry("csrfToken", "csrf-token-value")
                .containsKey("logoutUrl");
    }

    @Test
    @DisplayName("session_withPlainPrincipal_thenReturnsNameAndGeneratedCsrfToken")
    void session_withPlainPrincipal_thenReturnsNameAndGeneratedCsrfToken() {
        // when
        Map<String, String> body = controller
                .session(Mono.just(authentication("plain-user")), exchange(new MockWebSession()))
                .block();

        // then
        assertThat(body)
                .containsEntry("email", "plain-user")
                .containsKey("logoutUrl");
        assertThat(body.get("csrfToken")).isNotBlank();
    }

    @Test
    @DisplayName("session_withNoPrincipal_thenUnauthorized")
    void session_withNoPrincipal_thenUnauthorized() {
        // when / then
        assertThatThrownBy(() -> controller
                .session(Mono.empty(), exchange(new MockWebSession()))
                .block())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }

    @Test
    @DisplayName("session_withBearerJwtWithoutAnyIdentity_thenUnauthorizedInsteadOfCrashing")
    void session_withBearerJwtWithoutAnyIdentity_thenUnauthorizedInsteadOfCrashing() {
        // given: no email claim and no subject, so the authentication name is null too.
        var authentication = bearerToken(null, null);

        // when / then
        assertThatThrownBy(() -> controller
                .session(Mono.just(authentication), exchange(new MockWebSession()))
                .block())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401");
    }
}
