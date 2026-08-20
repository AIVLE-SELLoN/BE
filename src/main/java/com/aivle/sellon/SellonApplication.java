package com.aivle.sellon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaAuditing
@EntityScan(basePackages = "com.aivle.sellon.domain")
@EnableJpaRepositories(basePackages = "com.aivle.sellon.domain")
public class SellonApplication {
	public static void main(String[] args) {
		SpringApplication.run(SellonApplication.class, args);
	}
}
