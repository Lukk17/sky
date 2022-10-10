package com.lukk.sky.authservice;

import com.lukk.sky.authservice.config.ConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableConfigurationProperties(ConfigProperties.class)
@EnableTransactionManagement
@Slf4j
public class AuthServiceApplication {

    private final ConfigProperties configProperties;

    public AuthServiceApplication(ConfigProperties configProperties) {
        this.configProperties = configProperties;
    }

    public static void main(String[] args) {
        log.info("\n\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Auth service for Sky <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        SpringApplication.run(AuthServiceApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logProperties() {
        log.info("");
        log.info("-------------------- CONFIGURATION START --------------------");
        log.info("App name:\t\t\t\t\t\t {}", configProperties.getSpring().getApplication().getName());
        log.info("Application running on port:\t {}", configProperties.getServer().getPort());
        log.info("Spring logging level:\t\t\t {}", configProperties.getLogging().getLevel().getOrg().getSpringframework().getWeb());
        log.info("Hibernate logging level:\t\t {}", configProperties.getLogging().getLevel().getOrg().getHibernate());
        log.info("Database driver:\t\t\t\t {}", configProperties.getSpring().getDatasource().getDriverClassName());
        log.info("Hibernate operation type:\t\t {}", configProperties.getSpring().getJpa().getHibernate().getDdlAuto());
        String actuatorOpenEndpoints = configProperties.getManagement().getEndpoints().getWeb().getExposure().getInclude();
        log.info("Actuator open endpoints:\t\t {}", actuatorOpenEndpoints.equals("*") ? "ALL" : actuatorOpenEndpoints);
        String dbUrlSimplified = configProperties.getSpring().getDatasource().getUrl().split("\\?")[0];
        log.info("Database URL: {}", dbUrlSimplified);

        try {
            String dbParams = configProperties.getSpring().getDatasource().getUrl().split("\\?")[1];
            log.info("Database connections parameters: {}", dbParams);

        } catch (ArrayIndexOutOfBoundsException e) {
            log.info("Database has no connections parameters");
        }

        log.info("-------------------- CONFIGURATION END ----------------------");
        log.info("");
    }
}
