package com.microservices.hrms.config;

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
    
    public boolean isHR() {
        return hasRole("HR");
    }
    
    public boolean isManager() {
        return hasRole("MANAGER");
    }
    
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
    
    public boolean isEmployee() {
        return hasRole("USER");
    }
} 