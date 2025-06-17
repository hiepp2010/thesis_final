package com.microservices.ticketservice.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserPrincipal {
    private Long userId;
    private String username;
    private String email;
    private String roles;
    
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role.toUpperCase());
    }
    
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
    
    public boolean isUser() {
        return hasRole("USER");
    }
} 