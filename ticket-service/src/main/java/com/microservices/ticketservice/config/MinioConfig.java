package com.microservices.ticketservice.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {
    
    @Value("${minio.endpoint:http://minio:9000}")
    private String endpoint;
    
    @Value("${minio.access-key:minioadmin}")
    private String accessKey;
    
    @Value("${minio.secret-key:minioadmin123}")
    private String secretKey;
    
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
} 