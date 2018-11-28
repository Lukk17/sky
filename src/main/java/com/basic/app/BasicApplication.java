package com.basic.app;

import com.basic.interfaceService.StorageServiceInterface;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class BasicApplication {

	public static void main(String[] args) {

		SpringApplication.run(BasicApplication.class, args);
	}

	/**
	 * Initiate storage service to save uploaded by users files.
	 *
	 * @param storageServiceInterface
	 * @return 					initialize storage service
	 */
	@Bean
	CommandLineRunner init(StorageServiceInterface storageServiceInterface)
	{
		return (args ->
		{
			storageServiceInterface.init();
		});
	}
}
