package com.lukk.sky.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@AutoConfiguration(beforeName =
        "org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(JwtDecoder.class)
@Profile(LocalSecurityAutoConfiguration.LOCAL_PROFILE)
@Slf4j
public class LocalSecurityAutoConfiguration {

    static final String LOCAL_PROFILE = "local";

    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder localUnverifiedJwtDecoder() {
        log.warn("security.jwt_validation_disabled profile={} anyWellFormedTokenAccepted=true", LOCAL_PROFILE);

        return new UnverifiedJwtDecoder();
    }
}
