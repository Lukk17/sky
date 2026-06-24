package com.lukk.sky.common.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Auto-configuration that registers {@link CorrelationIdFilter} for every Spring Boot
 * web application that depends on sky-common.
 *
 * <p>The filter is ordered at {@link Ordered#HIGHEST_PRECEDENCE} so the MDC
 * {@code correlationId} is available before any other filter or interceptor runs,
 * including security filters.
 *
 * <p>Activated only for {@code @ConditionalOnWebApplication} contexts (servlet-based).
 * sky-notify uses Spring MVC (WebSocket + HTTP), so it qualifies. Pure reactive
 * (WebFlux) applications are excluded; a separate reactive filter would be needed there.
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CorrelationIdAutoConfiguration {

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilter() {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new CorrelationIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }
}
