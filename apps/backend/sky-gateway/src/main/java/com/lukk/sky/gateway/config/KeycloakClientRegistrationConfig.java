package com.lukk.sky.gateway.config;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * Static Keycloak client registration for the {@code !local} chain.
 *
 * <p>Built by hand rather than from {@code issuer-uri} discovery on purpose.
 * Discovery fetches {@code {issuer}/.well-known/openid-configuration} eagerly at
 * startup and rejects the document when its {@code issuer} differs from the requested
 * one. Behind this gateway those two can never agree at startup: the public issuer
 * ({@code http://localhost:5777/auth/realms/sky}) is served by Keycloak but answers
 * through this gateway's own {@code /auth} route, which is not listening yet while
 * the context refreshes, while the in-network address answers a document whose
 * issuer names the public URL. Every endpoint below defaults to the well-known
 * Keycloak layout under the configured issuer, with the server-to-server ones
 * overridable to the in-network address, so no HTTP call happens at startup at all.
 */
@Configuration
@EnableWebFluxSecurity
@Profile("!local")
public class KeycloakClientRegistrationConfig {

    @Bean
    public ReactiveClientRegistrationRepository keycloakClientRegistrationRepository(
            @Value("${KEYCLOAK_ISSUER_URI}") String issuerUri,
            @Value("${KEYCLOAK_AUTH_URI:${KEYCLOAK_ISSUER_URI}/protocol/openid-connect/auth}")
                    String authorizationUri,
            @Value("${KEYCLOAK_TOKEN_URI:${KEYCLOAK_ISSUER_URI}/protocol/openid-connect/token}")
                    String tokenUri,
            @Value("${KEYCLOAK_USER_INFO_URI:${KEYCLOAK_ISSUER_URI}/protocol/openid-connect/userinfo}")
                    String userInfoUri,
            @Value("${KEYCLOAK_JWK_SET_URI:${KEYCLOAK_ISSUER_URI}/protocol/openid-connect/certs}")
                    String jwkSetUri,
            @Value("${KEYCLOAK_END_SESSION_URI:${KEYCLOAK_ISSUER_URI}/protocol/openid-connect/logout}")
                    String endSessionUri,
            @Value("${KEYCLOAK_CLIENT_ID}") String clientId,
            @Value("${KEYCLOAK_CLIENT_SECRET}") String clientSecret) {
        ClientRegistration registration = ClientRegistration.withRegistrationId("keycloak")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "email", "profile")
                .authorizationUri(authorizationUri)
                .tokenUri(tokenUri)
                .userInfoUri(userInfoUri)
                .userNameAttributeName("preferred_username")
                .jwkSetUri(jwkSetUri)
                .issuerUri(issuerUri)
                .clientName("keycloak")
                .providerConfigurationMetadata(
                        Map.of("end_session_endpoint", endSessionUri))
                .build();
        return new InMemoryReactiveClientRegistrationRepository(List.of(registration));
    }
}
