package com.lukk.sky.common.swagger;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration that registers a {@link GroupedOpenApi} bean for every
 * sky service that has springdoc on its classpath.
 *
 * <p>The API title is controlled by {@code springdoc.info.title} in the
 * service's {@code application.yaml}. If absent, springdoc falls back to the
 * value of {@code spring.application.name}.
 *
 * <p>A service that needs custom group configuration can define its own
 * {@code GroupedOpenApi} bean; {@code @ConditionalOnMissingBean} ensures this
 * auto-configuration backs off in that case.
 *
 * <p>Activated only when {@code GroupedOpenApi} is on the classpath, so
 * sky-notify (which has no springdoc dependency) is unaffected.
 */
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
