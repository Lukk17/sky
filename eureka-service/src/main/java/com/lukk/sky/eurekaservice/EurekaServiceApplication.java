package com.lukk.sky.eurekaservice;

import com.lukk.sky.eurekaservice.config.ConfigProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;
import org.springframework.context.event.EventListener;

@SpringBootApplication
@EnableEurekaServer
@EnableConfigurationProperties(ConfigProperties.class)
@Slf4j
public class EurekaServiceApplication {

    private final ConfigProperties configProperties;

    public EurekaServiceApplication(ConfigProperties configProperties) {
        this.configProperties = configProperties;
    }

    public static void main(String[] args) {
        SpringApplication.run(EurekaServiceApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logProperties() {
        log.info("");
        log.info("-------------------- CONFIGURATION START --------------------");
        log.info("App name:\t\t\t\t\t\t {}", configProperties.getSpring().getApplication().getName());
        log.info("Application running on port:\t {}", configProperties.getServer().getPort());
        log.info("Spring logging level:\t\t\t {}", configProperties.getLogging().getLevel().getOrg().getSpringframework().getWeb());
        String actuatorOpenEndpoints = configProperties.getManagement().getEndpoints().getWeb().getExposure().getInclude();
        log.info("Actuator open endpoints:\t\t {}", actuatorOpenEndpoints.equals("*") ? "ALL" : actuatorOpenEndpoints);
        log.info("Naming service address: {}", configProperties.getEureka().getClient().getServiceUrl().getDefaultZone());
        log.info("-------------------- CONFIGURATION END ----------------------");
        log.info("");
    }
}
