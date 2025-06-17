package com.microservices.mail.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MailtrapResponse {
    
    private boolean success;
    
    @JsonProperty("message_ids")
    private List<String> messageIds;
    
    // Constructors
    public MailtrapResponse() {}
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public List<String> getMessageIds() {
        return messageIds;
    }
    
    public void setMessageIds(List<String> messageIds) {
        this.messageIds = messageIds;
    }
} 