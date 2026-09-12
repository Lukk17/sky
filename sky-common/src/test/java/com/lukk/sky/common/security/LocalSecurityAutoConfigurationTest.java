package com.lukk.sky.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LocalSecurityAutoConfiguration")
class LocalSecurityAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LocalSecurityAutoConfiguration.class));

    @Test
    @DisplayName("registersTheUnverifiedDecoder_whenTheLocalProfileIsActive")
    void registersTheUnverifiedDecoder_whenTheLocalProfileIsActive() {
        runner.withPropertyValues("spring.profiles.active=local")
                .run(context -> assertThat(context)
                        .hasSingleBean(JwtDecoder.class)
                        .getBean(JwtDecoder.class)
                        .isInstanceOf(UnverifiedJwtDecoder.class));
    }

    @Test
    @DisplayName("registersNothing_whenNoProfileIsActive")
    void registersNothing_whenNoProfileIsActive() {
        runner.run(context -> assertThat(context)
                .as("the unverified decoder must never exist outside the local profile")
                .doesNotHaveBean(JwtDecoder.class));
    }

    @Test
    @DisplayName("registersNothing_whenAnotherProfileIsActive")
    void registersNothing_whenAnotherProfileIsActive() {
        runner.withPropertyValues("spring.profiles.active=prod")
                .run(context -> assertThat(context)
                        .as("the unverified decoder must never exist outside the local profile")
                        .doesNotHaveBean(JwtDecoder.class));
    }

    @Test
    @DisplayName("keepsTheServiceDecoder_whenTheServiceAlreadyDefinesOne")
    void keepsTheServiceDecoder_whenTheServiceAlreadyDefinesOne() {
        runner.withPropertyValues("spring.profiles.active=local")
                .withUserConfiguration(ServiceSuppliedDecoderConfig.class)
                .run(context -> assertThat(context)
                        .hasSingleBean(JwtDecoder.class)
                        .getBean(JwtDecoder.class)
                        .as("a service wiring a verifying decoder must not be downgraded to the unverified one")
                        .isSameAs(ServiceSuppliedDecoderConfig.DECODER));
    }

    @Configuration(proxyBeanMethods = false)
    static class ServiceSuppliedDecoderConfig {

        static final JwtDecoder DECODER = token -> {
            throw new BadJwtException("the service decoder verifies, and this token is not signed");
        };

        @Bean
        JwtDecoder serviceSuppliedJwtDecoder() {
            return DECODER;
        }
    }
}
