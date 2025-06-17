package com.microservices.ticketservice;

import com.microservices.ticketservice.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
@EnableKafka
public class TicketServiceApplication {
    
    @Autowired
    private FileStorageService fileStorageService;

    public static void main(String[] args) {
        SpringApplication.run(TicketServiceApplication.class, args);
    }
    
    @PostConstruct
    public void init() {
        // Initialize MinIO buckets on startup
        fileStorageService.initBuckets();
    }
} 