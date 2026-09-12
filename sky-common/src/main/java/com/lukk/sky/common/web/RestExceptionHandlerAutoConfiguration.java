package com.lukk.sky.common.web;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@AutoConfiguration(beforeName = "org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(ResponseEntityExceptionHandler.class)
public class RestExceptionHandlerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ResponseEntityExceptionHandler.class)
    public SkyRestExceptionHandler skyRestExceptionHandler() {
        return new SkyRestExceptionHandler();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({PropertyReferenceException.class, DataAccessException.class})
    static class SpringDataExceptionHandlerConfiguration {

        @Bean
        @ConditionalOnMissingBean(SpringDataExceptionHandler.class)
        SpringDataExceptionHandler springDataExceptionHandler() {
            return new SpringDataExceptionHandler();
        }
    }
}
