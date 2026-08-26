package vgandolfi.dev.mana_paes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ManaPaesApplication {

	public static void main(String[] args) {
		SpringApplication.run(ManaPaesApplication.class, args);
	}

}
