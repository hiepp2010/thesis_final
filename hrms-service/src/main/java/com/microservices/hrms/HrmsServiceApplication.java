package com.microservices.hrms;

import com.microservices.hrms.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class HrmsServiceApplication {
    
    @Autowired
    private FileStorageService fileStorageService;

    public static void main(String[] args) {
        SpringApplication.run(HrmsServiceApplication.class, args);
    }
    
    @PostConstruct
    public void init() {
        // Initialize Minio buckets on startup
        fileStorageService.initBuckets();
    }
} 