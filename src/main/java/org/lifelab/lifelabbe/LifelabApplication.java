package org.lifelab.lifelabbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LifelabApplication {
	public static void main(String[] args) {
		SpringApplication.run(LifelabApplication.class, args);
	}
}


