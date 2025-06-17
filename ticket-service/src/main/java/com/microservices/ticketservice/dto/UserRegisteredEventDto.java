package com.microservices.ticketservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRegisteredEventDto {
    
    private Long userId;
    private String username;
    private String email;
    private String[] roles;
    private LocalDateTime timestamp;
    private String eventType; // Should be "USER_REGISTERED"
} 