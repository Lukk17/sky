package com.lukk.sky.common.swagger;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(GroupedOpenApi.class)
public class SwaggerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(GroupedOpenApi.class)
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("public")
                .pathsToMatch("/api/**", "/owners/**")
                .build();
    }
}
