package com.lukk;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

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
public class SkyApplication {

    public static void main(String[] args) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> App start <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        SpringApplication.run(SkyApplication.class, args);
    }

    @Configuration
    class RestTemplateConfig {

        // Create a bean for restTemplate to call services
        @Bean
        @LoadBalanced        // Load balance between service instances running at different ports.
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }
}
