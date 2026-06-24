package com.lukk.sky.booking;

import com.lukk.sky.booking.config.propertyBind.SpringConfigProperties;
import com.lukk.sky.booking.config.propertyBind.SkyConfigProperties;
import com.lukk.sky.common.config.LoggingLvlConfigProperties;
import com.lukk.sky.common.config.ManagementConfigProperties;
import com.lukk.sky.common.config.ServerConfigProperties;
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
public class SkyBookingApplication {

    private final SpringConfigProperties springConfigProperties;
    private final ManagementConfigProperties managementConfigProperties;
    private final ServerConfigProperties serverConfigProperties;
    private final LoggingLvlConfigProperties loggingLvlConfigProperties;
    private final SkyConfigProperties skyConfigProperties;

    public static void main(String[] args) {
        log.info(">>>>>>>>>> Booking App start <<<<<<<<<<");
        SpringApplication.run(SkyBookingApplication.class, args);
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

        String[] urlParts = springConfigProperties.getDatasource().url().split("\\?", 2);
        log.info("Database URL:\t\t\t\t {}", urlParts[0]);

        if (urlParts.length > 1) {
            log.info("Database connection params:\t\t {}", urlParts[1]);
        } else {
            log.info("Database has no connections parameters");
        }

        log.info("Spring logging level:\t\t {}", loggingLvlConfigProperties.getLevel().org().springframework().web());
        log.info("Hibernate logging level:\t\t {}", loggingLvlConfigProperties.getLevel().org().hibernate());

        log.info("Kafka server address:\t\t {}", springConfigProperties.getKafka().bootstrapServers());
        log.info("Kafka producer ID:\t\t\t {}", springConfigProperties.getKafka().producer().clientId());
        log.info("Kafka topics auto create:\t {}", springConfigProperties.getKafka().admin().autoCreate());

        log.info("Offer hostname is: {}", skyConfigProperties.getOfferServiceHostname());
        log.info("Offer port is: {}", skyConfigProperties.getOfferServiceHostPort());

        log.info("-------------------- CONFIGURATION END ----------------------");
        log.info("");
    }

}
