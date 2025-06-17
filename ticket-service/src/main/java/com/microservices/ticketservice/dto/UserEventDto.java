package com.microservices.ticketservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEventDto {
    
    private String eventType; // USER_CREATED, USER_UPDATED, USER_DELETED
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private long timestamp;
} 