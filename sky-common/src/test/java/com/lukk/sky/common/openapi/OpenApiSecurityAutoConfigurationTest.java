package com.lukk.sky.common.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static com.lukk.sky.common.openapi.OpenApiSecurityAutoConfiguration.BEARER_AUTH_SCHEME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("OpenApiSecurityAutoConfiguration")
class OpenApiSecurityAutoConfigurationTest {

    private final OpenApiSecurityAutoConfiguration config = new OpenApiSecurityAutoConfiguration();

    private final OpenApiServersProperties noServers = new OpenApiServersProperties(null);

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenApiSecurityAutoConfiguration.class));

    @Test
    @DisplayName("skyOpenApi_whenInvoked_thenRegistersHttpBearerSecurityScheme")
    void skyOpenApi_whenInvoked_thenRegistersHttpBearerSecurityScheme() {
        // when
        OpenAPI openAPI = config.skyOpenApi("Test API", "A test service.", "1.0", noServers);

        // then
        SecurityScheme scheme = openAPI.getComponents().getSecuritySchemes().get(BEARER_AUTH_SCHEME);

        assertThat(scheme)
                .as("bearerAuth security scheme must be registered")
                .isNotNull();
        assertThat(scheme.getType())
                .isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(scheme.getScheme())
                .isEqualTo("bearer");
        assertThat(scheme.getBearerFormat())
                .isEqualTo("JWT");
    }

    @Test
    @DisplayName("skyOpenApi_whenInvoked_thenAddsGlobalSecurityRequirement")
    void skyOpenApi_whenInvoked_thenAddsGlobalSecurityRequirement() {
        // when
        OpenAPI openAPI = config.skyOpenApi("Test API", "A test service.", "1.0", noServers);

        // then
        assertThat(openAPI.getSecurity())
                .as("a global security requirement must be present")
                .isNotEmpty();

        SecurityRequirement requirement = openAPI.getSecurity().get(0);

        assertThat(requirement.containsKey(BEARER_AUTH_SCHEME))
                .as("global security requirement must reference bearerAuth")
                .isTrue();
    }

    @Test
    @DisplayName("skyOpenApi_whenTitleProvided_thenInfoTitleMatchesInput")
    void skyOpenApi_whenTitleProvided_thenInfoTitleMatchesInput() {
        // given
        String expectedTitle = "Sky Offer API";

        // when
        OpenAPI openAPI = config.skyOpenApi(expectedTitle, "desc", "2.0", noServers);

        // then
        assertThat(openAPI.getInfo().getTitle())
                .isEqualTo(expectedTitle);
    }

    @Test
    @DisplayName("skyOpenApi_whenServersAreConfigured_thenTheDocumentCarriesThemInOrder")
    void skyOpenApi_whenServersAreConfigured_thenTheDocumentCarriesThemInOrder() {
        // given
        OpenApiServersProperties properties = new OpenApiServersProperties(List.of(
                new OpenApiServersProperties.ServerEntry("http://localhost:5552", "Local"),
                new OpenApiServersProperties.ServerEntry("https://sky.example.com", "Remote")));

        // when
        OpenAPI openAPI = config.skyOpenApi("Test API", "desc", "1.0", properties);

        // then
        assertThat(openAPI.getServers())
                .extracting(Server::getUrl, Server::getDescription)
                .containsExactly(
                        tuple("http://localhost:5552", "Local"),
                        tuple("https://sky.example.com", "Remote"));
    }

    @Test
    @DisplayName("skyOpenApi_whenNoServerIsConfigured_thenTheDocumentDeclaresNoneAndSpringdocStillGuesses")
    void skyOpenApi_whenNoServerIsConfigured_thenTheDocumentDeclaresNoneAndSpringdocStillGuesses() {
        // when
        OpenAPI openAPI = config.skyOpenApi("Test API", "desc", "1.0", noServers);

        // then
        assertThat(openAPI.getServers())
                .as("an empty list would still suppress the springdoc fallback, so it must stay unset")
                .isNull();
    }

    @Test
    @DisplayName("servers_whenBoundFromProperties_thenReachTheDocument")
    void servers_whenBoundFromProperties_thenReachTheDocument() {
        runner.withPropertyValues(
                        "springdoc.servers[0].url=http://localhost:5552",
                        "springdoc.servers[0].description=Local, straight at the service, bypassing the gateway")
                .run(context -> assertThat(context.getBean(OpenAPI.class).getServers())
                        .extracting(Server::getUrl, Server::getDescription)
                        .containsExactly(tuple(
                                "http://localhost:5552",
                                "Local, straight at the service, bypassing the gateway")));
    }
}
