
		package com.vionsys.hireai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
// @EnableJpaAuditing
@SpringBootApplication
public class HireaiBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(HireaiBackendApplication.class, args);
		System.out.println("Application running fine");
	}
}
