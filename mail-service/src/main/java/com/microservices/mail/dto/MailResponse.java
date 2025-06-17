package com.microservices.mail.dto;

import java.time.LocalDateTime;

public class MailResponse {
    
    private boolean success;
    private String message;
    private String messageId;
    private LocalDateTime sentAt;
    private String service;
    
    // Constructors
    public MailResponse() {}
    
    public MailResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.sentAt = LocalDateTime.now();
    }
    
    public MailResponse(boolean success, String message, String messageId) {
        this.success = success;
        this.message = message;
        this.messageId = messageId;
        this.sentAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public LocalDateTime getSentAt() {
        return sentAt;
    }
    
    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
    
    public String getService() {
        return service;
    }
    
    public void setService(String service) {
        this.service = service;
    }
} 