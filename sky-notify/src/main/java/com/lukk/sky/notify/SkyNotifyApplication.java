package com.lukk.sky.notify;

import com.lukk.sky.notify.config.propertyBind.LoggingLvlConfigProperties;
import com.lukk.sky.notify.config.propertyBind.ManagementConfigProperties;
import com.lukk.sky.notify.config.propertyBind.ServerConfigProperties;
import com.lukk.sky.notify.config.propertyBind.SpringConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.event.EventListener;

import static com.lukk.sky.notify.config.Constants.CONSUMER_GROUP_ID;

@SpringBootApplication
@ConfigurationPropertiesScan
@Slf4j
@RequiredArgsConstructor
public class SkyNotifyApplication {

    private final SpringConfigProperties springConfigProperties;
    private final ManagementConfigProperties managementConfigProperties;
    private final ServerConfigProperties serverConfigProperties;
    private final LoggingLvlConfigProperties loggingLvlConfigProperties;


    public static void main(String[] args) {
        log.info(">>>>>>>>>> Notify App start <<<<<<<<<<");
        SpringApplication.run(SkyNotifyApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logProperties() {
        log.info("");
        log.info("-------------------- CONFIGURATION START --------------------");
        log.info("Application running on port:\t {}", serverConfigProperties.getPort());

        log.info("App name:\t\t\t\t\t {}", springConfigProperties.getApplication().name());

        String actuatorOpenEndpoints = managementConfigProperties.getEndpoints().web().exposure().include();
        log.info("Actuator open endpoints:\t\t {}", actuatorOpenEndpoints.equals("*") ? "ALL" : actuatorOpenEndpoints);

        log.info("Spring logging level:\t\t {}", loggingLvlConfigProperties.getLevel().org().springframework().web());

        log.info("Kafka server address:\t\t {}", springConfigProperties.getKafka().bootstrapServers());
        log.info("Kafka consumer ID:\t\t\t {}", CONSUMER_GROUP_ID);
        log.info("Kafka topics auto create:\t {}", springConfigProperties.getKafka().admin().autoCreate());

        log.info("-------------------- CONFIGURATION END ----------------------");
        log.info("");
    }
}
