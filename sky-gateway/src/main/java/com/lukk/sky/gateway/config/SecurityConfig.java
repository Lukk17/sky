package com.lukk.sky.gateway.config;

import com.lukk.sky.common.security.SecurityPaths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    @Profile("local")
    public SecurityWebFilterChain permitAllSecurityWebFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable);
        return http.build();
    }

    @Bean
    @Profile("!local")
    public SecurityWebFilterChain oidcSecurityWebFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(SecurityPaths.probes().toArray(String[]::new)).permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2Login(login -> {
                })
                .csrf(ServerHttpSecurity.CsrfSpec::disable);
        return http.build();
    }
}
