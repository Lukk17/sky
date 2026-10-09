package com.lukk.sky.common.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@AutoConfiguration(after = ResourceServerJwtAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(SecurityFilterChain.class)
public class SecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain skySecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            SkySecurityCustomizer skySecurityCustomizer) throws Exception {
        return SkySecurityDefaults.filterChain(
                http,
                jwtAuthenticationConverter,
                SecurityPaths.probesAndApiDocs(),
                skySecurityCustomizer.additionalRules());
    }

    @Bean
    @ConditionalOnMissingBean(SkySecurityCustomizer.class)
    public SkySecurityCustomizer defaultSkySecurityCustomizer() {
        return () -> auth -> {
        };
    }

    public interface SkySecurityCustomizer {

        Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry> additionalRules();
    }
}
