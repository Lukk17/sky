package com.lukk.sky.common.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(JdbcConnectionDetails.class)
public class DatasourceCredentialsAutoConfiguration {

    @Bean
    static DatasourceCredentialsValidator datasourceCredentialsValidator() {
        return new DatasourceCredentialsValidator();
    }
}
