package com.lukk.sky.common.openapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpenApiAutoConfiguration")
class OpenApiAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenApiAutoConfiguration.class));

    @Test
    @DisplayName("publicApi_isGroupedAsPublic")
    void publicApi_isGroupedAsPublic() {
        runner.run(context -> assertThat(context.getBean(GroupedOpenApi.class).getGroup())
                .isEqualTo("public"));
    }

    @Test
    @DisplayName("publicApi_documentsOnlyTheApiAndOwnersPaths")
    void publicApi_documentsOnlyTheApiAndOwnersPaths() {
        runner.run(context -> assertThat(context.getBean(GroupedOpenApi.class).getPathsToMatch())
                .as("a wider match would publish the actuator endpoints as public API")
                .containsExactly("/api/**", "/owners/**"));
    }

    @Test
    @DisplayName("backsOff_whenTheServiceDeclaresItsOwnGroup")
    void backsOff_whenTheServiceDeclaresItsOwnGroup() {
        runner.withUserConfiguration(ServiceSuppliedGroupConfig.class)
                .run(context -> assertThat(context)
                        .hasSingleBean(GroupedOpenApi.class)
                        .getBean(GroupedOpenApi.class)
                        .isSameAs(ServiceSuppliedGroupConfig.GROUP));
    }

    @Test
    @DisplayName("registersNothing_whenSpringdocIsNotOnTheClasspath")
    void registersNothing_whenSpringdocIsNotOnTheClasspath() {
        runner.withClassLoader(new FilteredClassLoader(GroupedOpenApi.class))
                .run(context -> assertThat(context)
                        .as("sky-notify has no Swagger UI, so the group must not be created there")
                        .doesNotHaveBean(OpenApiAutoConfiguration.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class ServiceSuppliedGroupConfig {

        static final GroupedOpenApi GROUP = GroupedOpenApi.builder()
                .group("service-specific")
                .pathsToMatch("/internal/**")
                .build();

        @Bean
        GroupedOpenApi serviceSuppliedGroup() {
            return GROUP;
        }
    }
}
