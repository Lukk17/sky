package com.lukk;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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
public class SkyApplication {

    public static void main(String[] args) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> App start <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        SpringApplication.run(SkyApplication.class, args);
    }

}
