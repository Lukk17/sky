package com.lukk.sky.offer;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * Application startup
 * <p>
 * MUST BE in the highest level of packages folder's tree
 * if any component will be in different folder it must be added to ComponentScan:
 *
 * @ComponentScan(basePackageClasses = Component.class)
 */

@Log4j2
@SpringBootApplication
@EnableEurekaClient
public class SkyOfferApplication {

    public static void main(String[] args) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Offer App start <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        SpringApplication.run(SkyOfferApplication.class, args);
    }

}
