package com.lukk.sky.common.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CorrelationIdAutoConfiguration")
class CorrelationIdAutoConfigurationTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CorrelationIdAutoConfiguration.class));

    @Test
    @DisplayName("registersTheCorrelationIdFilter_forEveryRequestPath")
    void registersTheCorrelationIdFilter_forEveryRequestPath() {
        runner.run(context -> {
            FilterRegistrationBean<?> registration = context.getBean(FilterRegistrationBean.class);

            assertThat(registration.getFilter()).isInstanceOf(CorrelationIdFilter.class);
            assertThat(registration.getUrlPatterns()).containsExactly("/*");
        });
    }

    @Test
    @DisplayName("registersTheCorrelationIdFilter_aheadOfEveryOtherFilter")
    void registersTheCorrelationIdFilter_aheadOfEveryOtherFilter() {
        runner.run(context -> assertThat(context.getBean(FilterRegistrationBean.class).getOrder())
                .as("a later filter would log before the correlation id reaches the MDC")
                .isEqualTo(Ordered.HIGHEST_PRECEDENCE));
    }

    @Test
    @DisplayName("registersTheOutboundInterceptor_soDownstreamCallsCarryTheCorrelationId")
    void registersTheOutboundInterceptor_soDownstreamCallsCarryTheCorrelationId() {
        runner.run(context -> assertThat(context)
                .hasSingleBean(CorrelationIdClientHttpRequestInterceptor.class));
    }

    @Test
    @DisplayName("backsOff_whenTheServiceSuppliesItsOwnOutboundInterceptor")
    void backsOff_whenTheServiceSuppliesItsOwnOutboundInterceptor() {
        runner.withUserConfiguration(ServiceSuppliedInterceptorConfig.class)
                .run(context -> assertThat(context)
                        .hasSingleBean(CorrelationIdClientHttpRequestInterceptor.class)
                        .getBean(CorrelationIdClientHttpRequestInterceptor.class)
                        .isSameAs(ServiceSuppliedInterceptorConfig.INTERCEPTOR));
    }

    @Test
    @DisplayName("registersNothing_outsideAServletWebApplication")
    void registersNothing_outsideAServletWebApplication() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CorrelationIdAutoConfiguration.class))
                .run(context -> assertThat(context)
                        .doesNotHaveBean(FilterRegistrationBean.class)
                        .doesNotHaveBean(CorrelationIdClientHttpRequestInterceptor.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class ServiceSuppliedInterceptorConfig {

        static final CorrelationIdClientHttpRequestInterceptor INTERCEPTOR =
                new CorrelationIdClientHttpRequestInterceptor();

        @Bean
        CorrelationIdClientHttpRequestInterceptor serviceSuppliedInterceptor() {
            return INTERCEPTOR;
        }
    }
}
