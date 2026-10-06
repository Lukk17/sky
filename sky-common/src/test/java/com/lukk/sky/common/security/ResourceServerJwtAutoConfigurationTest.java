package com.lukk.sky.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResourceServerJwtAutoConfiguration")
class ResourceServerJwtAutoConfigurationTest {

    private final WebApplicationContextRunner webRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ResourceServerJwtAutoConfiguration.class));

    @Test
    @DisplayName("registersTheKeycloakRealmRoleConverter_inAServletWebApplication")
    void registersTheKeycloakRealmRoleConverter_inAServletWebApplication() {
        webRunner.run(context -> assertThat(context).hasSingleBean(JwtAuthenticationConverter.class));
    }

    @Test
    @DisplayName("registersNoJwtDecoder_soBootBuildsItFromTheIssuerUriProperty")
    void registersNoJwtDecoder_soBootBuildsItFromTheIssuerUriProperty() {
        webRunner.run(context -> assertThat(context).doesNotHaveBean(JwtDecoder.class));
    }

    @Test
    @DisplayName("registersNoAudienceValidator_whenOauth2AudienceIsUnset")
    void registersNoAudienceValidator_whenOauth2AudienceIsUnset() {
        webRunner.run(context -> assertThat(context).doesNotHaveBean(OAuth2TokenValidator.class));
    }

    @Test
    @DisplayName("registersNoAudienceValidator_whenOauth2AudienceIsBlank")
    void registersNoAudienceValidator_whenOauth2AudienceIsBlank() {
        webRunner.withPropertyValues("OAUTH2_AUDIENCE=")
                .run(context -> assertThat(context)
                        .as("an empty environment variable must not enforce an empty audience")
                        .doesNotHaveBean(OAuth2TokenValidator.class));
    }

    @Test
    @DisplayName("registersTheAudienceValidator_whenOauth2AudienceIsSet")
    void registersTheAudienceValidator_whenOauth2AudienceIsSet() {
        webRunner.withPropertyValues("OAUTH2_AUDIENCE=sky-backend")
                .run(context -> {
                    assertThat(context).hasSingleBean(OAuth2TokenValidator.class);

                    OAuth2TokenValidator<Jwt> validator = context.getBean(OAuth2TokenValidator.class);
                    Jwt jwt = Jwt.withTokenValue("token")
                            .header("alg", "none")
                            .claim("aud", List.of("sky-backend"))
                            .build();

                    assertThat(validator.validate(jwt).hasErrors())
                            .as("the registered validator must accept the configured audience")
                            .isFalse();
                });
    }

    @Test
    @DisplayName("registersNothing_outsideAServletWebApplication")
    void registersNothing_outsideAServletWebApplication() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ResourceServerJwtAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(JwtAuthenticationConverter.class));
    }
}
