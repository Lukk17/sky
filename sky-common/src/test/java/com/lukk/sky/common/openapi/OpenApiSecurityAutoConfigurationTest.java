package com.lukk.sky.common.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.lukk.sky.common.openapi.OpenApiSecurityAutoConfiguration.BEARER_AUTH_SCHEME;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OpenApiSecurityAutoConfiguration")
class OpenApiSecurityAutoConfigurationTest {

    private final OpenApiSecurityAutoConfiguration config = new OpenApiSecurityAutoConfiguration();

    @Test
    @DisplayName("skyOpenApi_whenInvoked_thenRegistersHttpBearerSecurityScheme")
    void skyOpenApi_whenInvoked_thenRegistersHttpBearerSecurityScheme() {
        // when
        OpenAPI openAPI = config.skyOpenApi("Test API", "A test service.", "1.0");

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
        OpenAPI openAPI = config.skyOpenApi("Test API", "A test service.", "1.0");

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
        OpenAPI openAPI = config.skyOpenApi(expectedTitle, "desc", "2.0");

        // then
        assertThat(openAPI.getInfo().getTitle())
                .isEqualTo(expectedTitle);
    }
}
