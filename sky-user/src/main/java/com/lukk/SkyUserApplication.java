package com.lukk;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@Log4j2
@SpringBootApplication
@EnableEurekaClient    // Enable eureka client. It inherits from @EnableDiscoveryClient.
public class SkyUserApplication {

    public static void main(String[] args) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> User App start <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        SpringApplication.run(SkyUserApplication.class, args);
    }
}
