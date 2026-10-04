package com.lukk.sky.gateway.config;

import com.lukk.sky.common.security.SecurityPaths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.oidc.web.server.logout.OidcClientInitiatedServerLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.web.server.DelegatingServerAuthenticationEntryPoint;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.csrf.CsrfWebFilter;
import org.springframework.security.web.server.csrf.WebSessionServerCsrfTokenRepository;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import reactor.core.publisher.Mono;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${sky-gateway.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    private CorsConfigurationSource frontendCorsConfigurationSource() {
        String allowedOrigin = validatedFrontendUrl(frontendUrl);
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-XSRF-TOKEN", "Accept"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // Local profile is loopback-only development: never bind it on a shared
    // network, it permits every exchange with no authentication.
    @Bean
    @Profile("local")
    public SecurityWebFilterChain permitAllSecurityWebFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .cors(cors -> cors.configurationSource(frontendCorsConfigurationSource()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable);
        return http.build();
    }

    @Bean
    @Profile("!local")
    public SecurityWebFilterChain oidcSecurityWebFilterChain(
            ServerHttpSecurity http,
            ReactiveClientRegistrationRepository clientRegistrationRepository,
            @Value("${sky-gateway.frontend-url:http://localhost:4200}") String frontendUrl) {
        http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(SecurityPaths.probes().toArray(String[]::new)).permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/offers", "/api/v1/offers/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/search").permitAll()
                        .anyExchange().authenticated()
                )
                .exceptionHandling(exceptions -> {
                    var entryPoint = new DelegatingServerAuthenticationEntryPoint(
                            new DelegatingServerAuthenticationEntryPoint.DelegateEntry(
                                    new PathPatternParserServerWebExchangeMatcher("/api/**"),
                                    new HttpStatusServerEntryPoint(UNAUTHORIZED)));
                    entryPoint.setDefaultEntryPoint(
                            new RedirectServerAuthenticationEntryPoint("/oauth2/authorization/keycloak"));
                    exceptions.authenticationEntryPoint(entryPoint);
                })
                .cors(cors -> cors.configurationSource(frontendCorsConfigurationSource()))
                .oauth2Login(login -> login.authenticationSuccessHandler(
                        new RedirectServerAuthenticationSuccessHandler(validatedFrontendUrl(frontendUrl))))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .logout(logout -> logout
                        .requiresLogout(new PathPatternParserServerWebExchangeMatcher("/logout", HttpMethod.POST))
                        .logoutSuccessHandler(oidcLogoutSuccessHandler(
                                clientRegistrationRepository, validatedFrontendUrl(frontendUrl))))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(sessionCsrfTokenRepository())
                        .requireCsrfProtectionMatcher(exchange -> {
                            // /logout is a full-browser form POST and must always carry
                            // the session CSRF token, even with an Authorization header.
                            if (exchange.getRequest().getPath().value().equals("/logout")) {
                                return ServerWebExchangeMatcher.MatchResult.match(java.util.Collections.emptyMap());
                            }
                            var headers = exchange.getRequest().getHeaders();
                            if (headers.getFirst(HttpHeaders.AUTHORIZATION) != null) {
                                return ServerWebExchangeMatcher.MatchResult.notMatch();
                            }
                            if (headers.getFirst(HttpHeaders.COOKIE) == null) {
                                return ServerWebExchangeMatcher.MatchResult.notMatch();
                            }
                            return CsrfWebFilter.DEFAULT_CSRF_MATCHER.matches(exchange);
                        }));
        return http.build();
    }

    private static WebSessionServerCsrfTokenRepository sessionCsrfTokenRepository() {
        var repository = new WebSessionServerCsrfTokenRepository();
        repository.setHeaderName("X-XSRF-TOKEN");
        return repository;
    }

    private static OidcClientInitiatedServerLogoutSuccessHandler oidcLogoutSuccessHandler(
            ReactiveClientRegistrationRepository clientRegistrationRepository, String frontendUrl) {
        var handler = new OidcClientInitiatedServerLogoutSuccessHandler(clientRegistrationRepository);
        handler.setPostLogoutRedirectUri(frontendUrl + "/home");
        return handler;
    }

    static String validatedFrontendUrl(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            throw new IllegalStateException("sky-gateway.frontend-url is not configured");
        }
        final java.net.URI uri;
        try {
            uri = java.net.URI.create(candidate.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("sky-gateway.frontend-url is not a valid URI: " + candidate, ex);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(java.util.Locale.ROOT);
        if (!("http".equals(scheme) || "https".equals(scheme)) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getFragment() != null) {
            throw new IllegalStateException(
                    "sky-gateway.frontend-url must be an absolute http(s) origin URL with no userinfo or fragment: "
                            + candidate);
        }
        return uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
    }
}
