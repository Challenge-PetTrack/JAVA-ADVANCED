package com.fiap.pettrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@EntityScan
@SpringBootApplication
public class PetTrackApplication {

	public static void main(String[] args) {
		SpringApplication.run(PetTrackApplication.class, args);
	}

}
