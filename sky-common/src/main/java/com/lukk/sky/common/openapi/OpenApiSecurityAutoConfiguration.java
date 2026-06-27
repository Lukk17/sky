package com.lukk.sky.common.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(GroupedOpenApi.class)
public class OpenApiSecurityAutoConfiguration {

    static final String BEARER_AUTH_SCHEME = "bearerAuth";

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI skyOpenApi(
            @Value("${springdoc.info.title:${spring.application.name:Sky API}}") String title,
            @Value("${springdoc.info.description:Sky platform REST API secured with Keycloak JWT bearer tokens.}") String description,
            @Value("${springdoc.info.version:1.0}") String version) {

        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Paste a Keycloak access token obtained via the Authorization Code flow.");

        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .description(description)
                        .version(version))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH_SCHEME, bearerScheme))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
    }
}
