package com.microservices.hrms.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Enumeration;

@RestController
@RequestMapping("/api/hrms")
public class HrmsController {

    @GetMapping("/headers")
    public ResponseEntity<Map<String, Object>> displayHeaders(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        
        // Get all headers
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headers.put(headerName, headerValue);
        }
        
        response.put("message", "Headers received from API Gateway");
        response.put("service", "HRMS Service");
        response.put("allHeaders", headers);
        
        // Specifically highlight the user headers from API Gateway
        Map<String, String> userHeaders = new HashMap<>();
        userHeaders.put("X-User-Id", request.getHeader("X-User-Id"));
        userHeaders.put("X-Username", request.getHeader("X-Username"));
        userHeaders.put("X-User-Roles", request.getHeader("X-User-Roles"));
        
        response.put("userHeadersFromGateway", userHeaders);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User profile from HRMS Service");
        response.put("userId", userId);
        response.put("username", username);
        response.put("roles", roles != null ? roles.split(",") : null);
        response.put("service", "HRMS Service");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/employees")
    public ResponseEntity<Map<String, Object>> getEmployees(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Username", required = false) String username,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        
        Map<String, Object> response = new HashMap<>();
        
        // Check if user has HR or ADMIN role
        boolean hasHRAccess = roles != null && (roles.contains("HR") || roles.contains("ADMIN"));
        
        if (!hasHRAccess) {
            response.put("error", "Access denied. HR or ADMIN role required.");
            response.put("yourRoles", roles);
            return ResponseEntity.status(403).body(response);
        }
        
        response.put("message", "Employee list retrieved successfully");
        response.put("requestedBy", username);
        response.put("userRoles", roles != null ? roles.split(",") : null);
        response.put("employees", new String[]{"John Doe", "Jane Smith", "Bob Johnson"});
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "HRMS Service");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
} 