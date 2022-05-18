package com.lukk.sky.message;

import com.lukk.sky.message.config.ConfigProperties;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.event.EventListener;

@SpringBootApplication
@EnableEurekaClient
@EnableConfigurationProperties(ConfigProperties.class)
@Slf4j
public class SkyMessageApplication {

    private final ConfigProperties configProperties;

    public SkyMessageApplication(ConfigProperties configProperties) {
        this.configProperties = configProperties;
    }

    public static void main(String[] args) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Message App start <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        SpringApplication.run(SkyMessageApplication.class, args);
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
        log.info("Naming service address: {}", configProperties.getEureka().getClient().getServiceUrl().getDefaultZone());
        String dbUrlSimplified = configProperties.getSpring().getDatasource().getUrl().split("\\?")[0];
        log.info("Database URL: {}", dbUrlSimplified);
        log.info("-------------------- CONFIGURATION END ----------------------");
        log.info("");
    }
}
