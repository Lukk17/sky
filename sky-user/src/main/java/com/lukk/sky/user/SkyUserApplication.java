package com.lukk.sky.user;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

// Need to scan for beans in another module (common) and in this module as well
@SpringBootApplication(scanBasePackages = {
        "com.lukk.sky.common",
        "com.lukk.sky.user"
})
@Log4j2
@EnableEurekaClient    // Enable eureka client
public class SkyUserApplication {

    public static void main(String[] args) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> User App for Sky start <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        SpringApplication.run(SkyUserApplication.class, args);
    }

}
