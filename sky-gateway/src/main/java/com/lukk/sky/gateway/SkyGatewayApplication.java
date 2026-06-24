package com.lukk.sky.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@Slf4j
public class SkyGatewayApplication {

    public static void main(String[] args) {
        log.info(">>>>>>>>>> Sky Gateway start <<<<<<<<<<");
        SpringApplication.run(SkyGatewayApplication.class, args);
    }
}
