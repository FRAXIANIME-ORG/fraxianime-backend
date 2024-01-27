package xyz.kiridepapel.fraxianimebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FraxianimeBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(FraxianimeBackendApplication.class, args);
	}

}
