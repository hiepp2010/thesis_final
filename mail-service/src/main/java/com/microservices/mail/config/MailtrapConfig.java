package com.microservices.mail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConfigurationProperties(prefix = "spring.mailtrap")
public class MailtrapConfig {
    
    private String apiToken;
    private String apiUrl;
    private String defaultRecipient;
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    // Getters and Setters
    public String getApiToken() {
        return apiToken;
    }
    
    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }
    
    public String getApiUrl() {
        return apiUrl;
    }
    
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
    
    public String getDefaultRecipient() {
        return defaultRecipient;
    }
    
    public void setDefaultRecipient(String defaultRecipient) {
        this.defaultRecipient = defaultRecipient;
    }
} 