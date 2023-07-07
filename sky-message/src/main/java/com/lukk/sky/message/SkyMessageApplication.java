package com.lukk.sky.message;

import com.lukk.sky.message.config.propertyBind.LoggingLvlConfigProperties;
import com.lukk.sky.message.config.propertyBind.ManagementConfigProperties;
import com.lukk.sky.message.config.propertyBind.ServerConfigProperties;
import com.lukk.sky.message.config.propertyBind.SpringConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableTransactionManagement
@Slf4j
@RequiredArgsConstructor
public class SkyMessageApplication {

    private final SpringConfigProperties springConfigProperties;
    private final ManagementConfigProperties managementConfigProperties;
    private final ServerConfigProperties serverConfigProperties;
    private final LoggingLvlConfigProperties loggingLvlConfigProperties;

    public static void main(String[] args) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Message App start <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        SpringApplication.run(SkyMessageApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logProperties() {
        log.info("");
        log.info("-------------------- CONFIGURATION START --------------------");
        log.info("Application running on port:\t {}", serverConfigProperties.getPort());

        log.info("App name:\t\t\t\t\t {}", springConfigProperties.getApplication().name());
        log.info("Database driver:\t\t\t\t {}", springConfigProperties.getDatasource().driverClassName());
        log.info("Hibernate operation type:\t {}", springConfigProperties.getJpa().hibernate().ddlAuto());

        String actuatorOpenEndpoints = managementConfigProperties.getEndpoints().web().exposure().include();
        log.info("Actuator open endpoints:\t\t {}", actuatorOpenEndpoints.equals("*") ? "ALL" : actuatorOpenEndpoints);

        String dbUrlSimplified = springConfigProperties.getDatasource().url().split("\\?")[0];
        log.info("Database URL:\t\t\t\t {}", dbUrlSimplified);

        try {
            String dbParams = springConfigProperties.getDatasource().url().split("\\?")[1];
            log.info("Database connection params:\t\t {}", dbParams);

        } catch (ArrayIndexOutOfBoundsException e) {
            log.info("Database has no connections parameters");
        }

        log.info("Spring logging level:\t\t {}", loggingLvlConfigProperties.getLevel().org().springframework().web());
        log.info("Hibernate logging level:\t\t {}", loggingLvlConfigProperties.getLevel().org().hibernate());

        log.info("-------------------- CONFIGURATION END ----------------------");
        log.info("");
    }
}
