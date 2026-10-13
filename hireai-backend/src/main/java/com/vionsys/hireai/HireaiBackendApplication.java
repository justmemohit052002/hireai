package com.vionsys.hireai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication
public class HireaiBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(HireaiBackendApplication.class, args);
		System.out.println("Application running fine");
	}
}
