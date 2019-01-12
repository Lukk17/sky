package com.basic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 *
 * Application startup
 *
 * MUST BE in the highest level of packages folder's tree
 * if any component will be in different folder it must be added to ComponentScan:
 * @ComponentScan(basePackageClasses = Component.class)
 *
 */

@SpringBootApplication
public class BasicApplication {

	public static void main(String[] args) {

		SpringApplication.run(BasicApplication.class, args);
	}

}