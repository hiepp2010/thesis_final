package com.microservices.auth.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("refresh_tokens")
public class RefreshToken {
    
    @Id
    private String token;
    
    @Indexed
    private Long userId;
    
    @Indexed
    private String username;
    
    private String deviceInfo; // Optional: track device/browser
    private LocalDateTime createdAt;
    private LocalDateTime lastUsedAt;
    
    @TimeToLive
    private Long ttl = 604800L; // Time to live in seconds (7 days = 604800)
    
    // Custom constructor for creating new tokens
    public RefreshToken(String token, Long userId, String username, String deviceInfo) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.deviceInfo = deviceInfo;
        this.createdAt = LocalDateTime.now();
        this.lastUsedAt = LocalDateTime.now();
        this.ttl = 604800L; // 7 days in seconds
    }
    
    // Helper method to update last used timestamp
    public void updateLastUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
} 