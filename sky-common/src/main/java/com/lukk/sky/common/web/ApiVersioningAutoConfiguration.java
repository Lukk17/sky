package com.lukk.sky.common.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuration that enables Spring Framework 7 native API versioning for all
 * servlet-based sky services.
 *
 * <p>Strategy: path-segment at index 1, matching the "/api/v1/..." URL structure.
 * Only paths whose second segment starts with "v" followed by a digit are treated as versioned
 * (predicate via {@code PathApiVersionResolver(int, Predicate)}). Paths like
 * {@code /api/internal/...} are left unversioned and served regardless of version.
 *
 * <p>The default version is set to "1" so requests to unversioned paths (actuator, Swagger,
 * internal endpoints) are never rejected with {@code MissingApiVersionException}.
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ApiVersioningAutoConfiguration implements WebMvcConfigurer {

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer
            .usePathSegment(1, path -> {
                var elements = path.pathWithinApplication().elements();
                int i = 0;
                for (var element : elements) {
                    if (element instanceof org.springframework.http.server.PathContainer.PathSegment seg) {
                        if (i == 1) {
                            String value = seg.value();
                            return value.length() >= 2
                                    && value.charAt(0) == 'v'
                                    && Character.isDigit(value.charAt(1));
                        }
                        i++;
                    }
                }
                return false;
            })
            .setDefaultVersion("1")
            .addSupportedVersions("1");
    }
}
