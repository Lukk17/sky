package com.lukk.sky.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MethodSecurityAutoConfiguration")
class MethodSecurityAutoConfigurationTest {

    private static final String GUARDED_RESULT = "offer-42";

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MethodSecurityAutoConfiguration.class))
            .withUserConfiguration(GuardedServiceConfig.class);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("guardedMethod_runs_whenTheCallerHoldsTheUserRole")
    void guardedMethod_runs_whenTheCallerHoldsTheUserRole() {
        runner.run(context -> {
            givenAuthority("ROLE_USER");

            assertThat(context.getBean(GuardedService.class).readOffer()).isEqualTo(GUARDED_RESULT);
        });
    }

    @Test
    @DisplayName("guardedMethod_runs_whenTheCallerHoldsTheAdminRole")
    void guardedMethod_runs_whenTheCallerHoldsTheAdminRole() {
        runner.run(context -> {
            givenAuthority("ROLE_ADMIN");

            assertThat(context.getBean(GuardedService.class).readOffer()).isEqualTo(GUARDED_RESULT);
        });
    }

    @Test
    @DisplayName("guardedMethod_isDenied_whenTheCallerHoldsAnotherRole")
    void guardedMethod_isDenied_whenTheCallerHoldsAnotherRole() {
        runner.run(context -> {
            givenAuthority("ROLE_SERVICE");
            GuardedService service = context.getBean(GuardedService.class);

            assertThatThrownBy(service::readOffer)
                    .as("@IsUser must reject a realm role it does not list")
                    .isInstanceOf(AccessDeniedException.class);
        });
    }

    @Test
    @DisplayName("guardedMethod_isDenied_whenThereIsNoAuthentication")
    void guardedMethod_isDenied_whenThereIsNoAuthentication() {
        runner.run(context -> {
            GuardedService service = context.getBean(GuardedService.class);

            assertThatThrownBy(service::readOffer)
                    .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        });
    }

    @Test
    @DisplayName("unguardedMethod_runs_whenThereIsNoAuthentication")
    void unguardedMethod_runs_whenThereIsNoAuthentication() {
        runner.run(context -> assertThat(context.getBean(GuardedService.class).readPublicInfo())
                .as("method security must not close a method nobody annotated")
                .isEqualTo("public"));
    }

    @Test
    @DisplayName("appliesNoMethodSecurity_whenTheAutoConfigurationIsAbsent")
    void appliesNoMethodSecurity_whenTheAutoConfigurationIsAbsent() {
        new WebApplicationContextRunner()
                .withUserConfiguration(GuardedServiceConfig.class)
                .run(context -> assertThat(context.getBean(GuardedService.class).readOffer())
                        .as("the guard has to come from this auto-configuration, or the tests above prove nothing")
                        .isEqualTo(GUARDED_RESULT));
    }

    @Test
    @DisplayName("registersNothing_outsideAServletWebApplication")
    void registersNothing_outsideAServletWebApplication() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MethodSecurityAutoConfiguration.class))
                .run(context -> assertThat(context).doesNotHaveBean(MethodSecurityAutoConfiguration.class));
    }

    private static void givenAuthority(String authority) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "caller@sky.test",
                        "n/a",
                        List.of(new SimpleGrantedAuthority(authority))));
    }

    @Configuration(proxyBeanMethods = false)
    static class GuardedServiceConfig {

        @Bean
        GuardedService guardedService() {
            return new GuardedService();
        }
    }

    static class GuardedService {

        @IsUser
        public String readOffer() {
            return GUARDED_RESULT;
        }

        public String readPublicInfo() {
            return "public";
        }
    }
}
