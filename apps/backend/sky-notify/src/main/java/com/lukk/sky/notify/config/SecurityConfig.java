package com.lukk.sky.notify.config;

import com.lukk.sky.common.security.SecurityAutoConfiguration.SkySecurityCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {

    @Bean
    public SkySecurityCustomizer notifySecurityCustomizer() {
        return () -> auth -> auth
                .requestMatchers("/notifyWebsocket/**").permitAll();
    }
}
