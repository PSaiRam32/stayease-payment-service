package com.stayease.payment_service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.stayease")
@Slf4j
public class PaymentServiceApplication {

	public static void main(String[] args) {
		log.info("Starting Payment  Service Application");
		SpringApplication.run(PaymentServiceApplication.class, args);
		log.info("Payment Service Application Started Successfully on port 8086");
		log.info("API Documentation: http://localhost:8086/swagger-ui.html");
	}

}
