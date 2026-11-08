package com.lukk.sky.common.config;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;

public class DatasourceCredentialsValidator implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof JdbcConnectionDetails connection) {
            RequiredCredentials.check()
                    .and("spring.datasource.username", connection.getUsername())
                    .and("spring.datasource.password", connection.getPassword())
                    .orFailStartup();
        }

        return bean;
    }
}
