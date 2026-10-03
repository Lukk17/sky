package com.lukk.sky.common.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(WebMvcConfigurer.class)
@ConditionalOnProperty(name = "sky.crossOrigin.allowed")
public class CrossOriginAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    public static class CrossOriginWebMvcConfigurer implements WebMvcConfigurer {

        private final String allowedOrigins;

        public CrossOriginWebMvcConfigurer(@Value("${sky.crossOrigin.allowed}") String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/api/**")
                    .allowedOrigins(allowedOrigins.split("\\s*,\\s*"))
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("Authorization", "Content-Type", "X-XSRF-TOKEN")
                    .allowCredentials(true);
        }
    }
}
