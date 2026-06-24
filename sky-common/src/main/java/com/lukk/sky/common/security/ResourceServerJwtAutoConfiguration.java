package com.lukk.sky.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto-configuration that supplies a {@link JwtDecoder} for any web service that depends on
 * sky-common and adds {@code spring-boot-starter-oauth2-resource-server} to its classpath.
 *
 * <p>{@code @ConditionalOnMissingBean(JwtDecoder.class)} lets a test's
 * {@code @TestConfiguration} provide a stub decoder without triggering the OIDC discovery
 * HTTP call that {@link NimbusJwtDecoder#withIssuerLocation} makes at build time.
 *
 * <p>Audience validation is opt-in: set {@code OAUTH2_AUDIENCE} to a non-blank value to
 * enable the {@link AudienceValidator}. Leave the environment variable unset (or blank) to
 * skip audience validation, which keeps existing deployments working without changes.
 */
@AutoConfiguration
@ConditionalOnWebApplication
public class ResourceServerJwtAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder resourceServerJwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${OAUTH2_AUDIENCE:}") String audience) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(JwtValidators.createDefaultWithIssuer(issuerUri));

        if (audience != null && !audience.isBlank()) {
            validators.add(new AudienceValidator(audience));
        }

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));

        return decoder;
    }
}
