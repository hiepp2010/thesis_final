package com.microservices.auth.event;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserEvent {
    
    @JsonProperty("eventType")
    private String eventType; // USER_CREATED, USER_UPDATED, USER_DELETED
    
    @JsonProperty("userId")
    private Long userId;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("fullName")
    private String fullName;
    
    @JsonProperty("timestamp")
    private long timestamp;

    // Default constructor
    public UserEvent() {}

    // Constructor
    public UserEvent(String eventType, Long userId, String username, String email, String fullName) {
        this.eventType = eventType;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "UserEvent{" +
                "eventType='" + eventType + '\'' +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
} 