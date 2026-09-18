package com.lukk.sky.notify.config;

import com.lukk.sky.common.security.SecurityPaths;
import com.lukk.sky.common.security.SkySecurityDefaults;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain notifySecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {

        return SkySecurityDefaults.filterChain(http, jwtAuthenticationConverter,
                SecurityPaths.probesPlus("/notifyWebsocket/**"));
    }
}
