package com.lukk.sky.offer.config;

import com.lukk.sky.common.security.SecurityAutoConfiguration.SkySecurityCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

@Configuration
public class SecurityConfig {

    @Bean
    public SkySecurityCustomizer offerSecurityCustomizer() {
        return () -> auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/v1/offers/*/owner").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/offers", "/api/v1/offers/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/search").permitAll()
                .requestMatchers("/api/v1/owner/**").authenticated();
    }
}
