package com.almacen.warnes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WarnesApplication {

	public static void main(String[] args) {
		SpringApplication.run(WarnesApplication.class, args);
	}

}
