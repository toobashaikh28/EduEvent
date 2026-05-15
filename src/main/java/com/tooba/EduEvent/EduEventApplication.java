package com.tooba.EduEvent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EduEventApplication {

	public static void main(String[] args) {
		SpringApplication.run(EduEventApplication.class, args);
	}

}
