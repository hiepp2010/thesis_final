package com.microservices.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// No need to exclude WebMvcAutoConfiguration if spring.main.web-application-type=reactive is set in properties
// and spring-boot-starter-webflux is present.
@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
} 