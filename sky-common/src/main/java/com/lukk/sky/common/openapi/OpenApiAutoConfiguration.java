package com.lukk.sky.common.openapi;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(GroupedOpenApi.class)
public class OpenApiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(GroupedOpenApi.class)
    public GroupedOpenApi publicApi(
            @Value("${springdoc.info.title:${spring.application.name:Sky API}}") String title) {

        return GroupedOpenApi.builder()
                .group("public")
                .displayName(title)
                .pathsToMatch("/api/**", "/owners/**")
                .build();
    }
}
