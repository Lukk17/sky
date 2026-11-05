package com.lukk.sky.gateway.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(SecurityConfigOidcProfileTest.BearerTokenDecoder.class)
class SecurityConfigOidcProfileTest {

    private static final StubOidcProvider OIDC_PROVIDER = StubOidcProvider.start();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private RouteLocator routeLocator;

    private WebTestClient client;

    @TestConfiguration(proxyBeanMethods = false)
    static class BearerTokenDecoder {

        @Bean
        @Primary
        ReactiveJwtDecoder bearerTokenReactiveJwtDecoder() {
            return token -> Mono.just(switch (token) {
                case "no-identity-token" -> Jwt.withTokenValue(token)
                        .header("alg", "none")
                        .claim("iss", "sky-test")
                        .build();
                default -> Jwt.withTokenValue(token)
                        .header("alg", "none")
                        .subject("bearer-user")
                        .claim("email", "bearer@sky.dev")
                        .build();
            });
        }
    }

    @DynamicPropertySource
    static void keycloakProperties(DynamicPropertyRegistry registry) {
        registry.add("KEYCLOAK_ISSUER_URI", OIDC_PROVIDER::issuerUri);
        registry.add("KEYCLOAK_CLIENT_ID", () -> "sky-gateway");
        registry.add("KEYCLOAK_CLIENT_SECRET", () -> "stub-client-secret");
    }

    @AfterAll
    static void stopOidcProvider() {
        OIDC_PROVIDER.close();
    }

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    @DisplayName("prometheus_whenNoCredentialsSupplied_thenRedirectsToLoginInsteadOfExposingMetrics")
    void prometheus_whenNoCredentialsSupplied_thenRedirectsToLoginInsteadOfExposingMetrics() {
        // when / then
        client.get().uri("/actuator/prometheus")
                .exchange()
                .expectStatus().isFound()
                .expectHeader().value("Location", location ->
                        assertThat(location).contains("/oauth2/authorization/keycloak"))
                .expectBody().isEmpty();
    }

    @Test
    @DisplayName("health_whenNoCredentialsSupplied_thenReturnsOk")
    void health_whenNoCredentialsSupplied_thenReturnsOk() {
        // when / then
        client.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("livenessProbe_whenNoCredentialsSupplied_thenReturnsOk")
    void livenessProbe_whenNoCredentialsSupplied_thenReturnsOk() {
        // when / then
        client.get().uri("/actuator/health/liveness")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("readinessProbe_whenNoCredentialsSupplied_thenReturnsOk")
    void readinessProbe_whenNoCredentialsSupplied_thenReturnsOk() {
        // when / then
        client.get().uri("/actuator/health/readiness")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("info_whenNoCredentialsSupplied_thenReturnsOk")
    void info_whenNoCredentialsSupplied_thenReturnsOk() {
        // when / then
        client.get().uri("/actuator/info")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("session_whenNoCredentialsSupplied_thenUnauthorizedInsteadOfRedirect")
    void session_whenNoCredentialsSupplied_thenUnauthorizedInsteadOfRedirect() {
        // when / then
        client.get().uri("/api/session")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();
    }

    @Test
    @DisplayName("session_whenBearerTokenSupplied_thenReturnsEmailAndCsrfToken")
    void session_whenBearerTokenSupplied_thenReturnsEmailAndCsrfToken() {
        // when / then
        client.get().uri("/api/session")
                .headers(headers -> headers.setBearerAuth("bearer-token"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo("bearer@sky.dev")
                .jsonPath("$.csrfToken").isNotEmpty()
                .jsonPath("$.logoutUrl").value(url ->
                        assertThat(url.toString()).contains("openid-connect/logout"));
    }

    @Test
    @DisplayName("session_whenBearerTokenHasNoIdentity_thenUnauthorizedInsteadOfServerError")
    void session_whenBearerTokenHasNoIdentity_thenUnauthorizedInsteadOfServerError() {
        // when / then
        client.get().uri("/api/session")
                .headers(headers -> headers.setBearerAuth("no-identity-token"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("anonymousSearchGet_whenNoCredentialsSupplied_thenPassesSecurityToRouting")
    void anonymousSearchGet_whenNoCredentialsSupplied_thenPassesSecurityToRouting() {
        // when / then: search is public by method and GET is a safe method, so the
        // chain neither authenticates it nor runs the CSRF gate. The test upstream is
        // a dead port, so reaching the route answers 5xx or 404; 401 and 403 would
        // mean public search was rejected again.
        client.get().uri("/api/v1/search?term=Paris")
                .exchange()
                .expectStatus().value(status -> assertThat(status).isIn(404, 500, 502, 503));
    }

    @Test
    @DisplayName("protectedMutation_whenSessionCookieWithoutCsrfToken_thenStillRejectedWithForbidden")
    void protectedMutation_whenSessionCookieWithoutCsrfToken_thenStillRejectedWithForbidden() {
        // given: the same browser session cookie, with no CSRF header
        ResponseCookie sessionCookie = sessionCookie();

        // when / then: the chain has no search exemption, so every mutating API path
        // with the session cookie still requires the session CSRF token.
        client.post().uri("/api/v1/offers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .cookie("SESSION", sessionCookie.getValue())
                .exchange()
                .expectStatus().isForbidden();
    }

    private ResponseCookie sessionCookie() {
        var result = client.get().uri("/api/session")
                .headers(headers -> headers.setBearerAuth("bearer-token"))
                .exchange()
                .expectStatus().isOk()
                .returnResult(Void.class);
        ResponseCookie sessionCookie = result.getResponseCookies().getFirst("SESSION");
        assertThat(sessionCookie).isNotNull();
        assertThat(sessionCookie.getValue()).isNotBlank();
        return sessionCookie;
    }

    @Test
    @DisplayName("proxiedRoute_whenNoCredentialsSupplied_thenUnauthorizedInsteadOfRedirect")
    void proxiedRoute_whenNoCredentialsSupplied_thenUnauthorizedInsteadOfRedirect() {
        // when / then
        client.get().uri("/api/v1/bookings")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/booking/swagger-ui.html",
            "/booking/swagger-ui/index.html",
            "/booking/v3/api-docs",
            "/offer/swagger-ui.html",
            "/offer/swagger-ui/index.html",
            "/offer/v3/api-docs",
            "/msg/swagger-ui.html",
            "/msg/swagger-ui/index.html",
            "/msg/v3/api-docs"
    })
    void documentationPath_whenNoCredentialsSupplied_thenRedirectsToLoginInsteadOfServingTheDocs(String path) {
        // when / then
        client.get().uri(path)
                .exchange()
                .expectStatus().isFound()
                .expectHeader().value("Location", location ->
                        assertThat(location).contains("/oauth2/authorization/keycloak"));
    }

    @Test
    @DisplayName("logout_whenGetRequest_thenDoesNotRunOidcLogout")
    void logout_whenGetRequest_thenDoesNotRunOidcLogout() {
        // when / then
        client.get().uri("/logout")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("logout_whenPostRequestWithoutCsrfToken_thenRejectedBeforeOidcLogoutInsteadOf404")
    void logout_whenPostRequestWithoutCsrfToken_thenRejectedBeforeOidcLogoutInsteadOf404() {
        // when / then
        client.post().uri("/logout")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("keycloakPath_whenPostRequestWithoutCsrfToken_thenPassesSecurityToRoutingInsteadOf403")
    void keycloakPath_whenPostRequestWithoutCsrfToken_thenPassesSecurityToRoutingInsteadOf403() {
        // when / then: upstream is a dead port under test, so reaching the route
        // answers 5xx; a 403 would mean the gateway session CSRF gate fired,
        // which must never gate Keycloak browser form posts.
        client.post().uri("/auth/realms/sky/login-actions/authenticate")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("keycloakPath_whenPostRequestWithOpaqueOrigin_thenPassesCorsToRoutingInsteadOf403")
    void keycloakPath_whenPostRequestWithOpaqueOrigin_thenPassesCorsToRoutingInsteadOf403() {
        // when / then: privacy-hardened browsers send Origin null on the Keycloak
        // login post; the gateway must echo it rather than answer 403, and the dead
        // upstream answers 5xx to prove the request was routed.
        client.post().uri("/auth/realms/sky/login-actions/authenticate")
                .header("Origin", "null")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("storePath_whenPostRequestWithoutCsrfToken_thenNotRejectedByCsrfGate")
    void storePath_whenPostRequestWithoutCsrfToken_thenNotRejectedByCsrfGate() {
        // when / then: POST /s3 needs a session, so an anonymous call redirects to
        // login; a 403 would mean the gateway session CSRF gate fired, which must
        // never gate store fetches (presigned capability URLs carry their own auth).
        client.post().uri("/s3/sky-offers/offers/probe")
                .exchange()
                .expectStatus().is3xxRedirection();
    }

    @Test
    @DisplayName("loginCallback_whenAuthorizationRequestIsUnknown_thenRedirectsToFrontendInsteadOfLoginErrorPage")
    void loginCallback_whenAuthorizationRequestIsUnknown_thenRedirectsToFrontendInsteadOfLoginErrorPage() {
        // when / then: a failed code exchange must return to the frontend where
        // the login button can retry; the default /login?error target names a
        // path no gateway route serves.
        client.get().uri("/login/oauth2/code/keycloak?code=bogus&state=bogus")
                .exchange()
                .expectStatus().isFound()
                .expectHeader().value("Location", location ->
                        assertThat(location).startsWith("http://localhost:4200"));
    }

    @Test
    @DisplayName("routeTable_whenTheOidcDocumentIsActive_thenCarriesEveryDocumentationRouteTheLocalDocumentHas")
    void routeTable_whenTheOidcDocumentIsActive_thenCarriesEveryDocumentationRouteTheLocalDocumentHas() {
        // given
        List<String> routeIds = routeLocator.getRoutes()
                .map(Route::getId)
                .collectList()
                .block();

        // when / then
        assertThat(routeIds).contains(
                "booking-swagger-route",
                "booking-api-docs-route",
                "offer-swagger-route",
                "offer-api-docs-route",
                "message-swagger-route",
                "message-api-docs-route",
                "keycloak-route",
                "s3-route");
    }
}
