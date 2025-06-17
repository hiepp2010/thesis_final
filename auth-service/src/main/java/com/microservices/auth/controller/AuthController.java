package com.microservices.auth.controller;

import com.microservices.auth.dto.AuthResponse;
import com.microservices.auth.dto.LoginRequest;
import com.microservices.auth.dto.RegisterRequest;
import com.microservices.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Authentication", description = "APIs for user authentication, registration, and JWT token management")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Operation(summary = "User Login", description = "Authenticate user with username/email and password")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class),
            examples = @ExampleObject(value = """
                {
                  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                  "tokenType": "Bearer",
                  "userId": 1,
                  "username": "john.doe",
                  "email": "john@example.com",
                  "roles": ["USER"]
                }
                """))),
        @ApiResponse(responseCode = "400", description = "Invalid credentials or validation error",
            content = @Content(schema = @Schema(example = """
                {
                  "error": "Invalid credentials"
                }
                """)))
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Parameter(description = "Login credentials", required = true,
                examples = @ExampleObject(value = """
                    {
                      "username": "john.doe",
                      "password": "password123"
                    }
                    """))
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {
        try {
            String deviceInfo = getDeviceInfo(request);
            AuthResponse authResponse = authService.login(loginRequest, deviceInfo);
            return ResponseEntity.ok(authResponse);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "User Registration", description = "Register a new user account")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Registration successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Username or email already exists, or validation error",
            content = @Content(schema = @Schema(example = """
                {
                  "error": "Username is already taken"
                }
                """)))
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Parameter(description = "User registration data", required = true,
                examples = @ExampleObject(value = """
                    {
                      "username": "john.doe",
                      "email": "john@example.com",
                      "password": "password123",
                      "firstName": "John",
                      "lastName": "Doe"
                    }
                    """))
            @Valid @RequestBody RegisterRequest registerRequest,
            HttpServletRequest request) {
        try {
            String deviceInfo = getDeviceInfo(request);
            AuthResponse authResponse = authService.register(registerRequest, deviceInfo);
            return ResponseEntity.ok(authResponse);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "Refresh Access Token", description = "Generate new access token using refresh token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token",
            content = @Content(schema = @Schema(example = """
                {
                  "error": "Invalid refresh token"
                }
                """)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @Parameter(description = "Refresh token request", required = true,
                examples = @ExampleObject(value = """
                    {
                      "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                    }
                    """))
            @RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Refresh token is required");
                return ResponseEntity.badRequest().body(error);
            }

            AuthResponse authResponse = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(authResponse);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "Validate JWT Token", description = "Validate a JWT token and extract user information")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token validation result",
            content = @Content(schema = @Schema(example = """
                {
                  "valid": true,
                  "username": "john.doe",
                  "roles": ["USER"]
                }
                """))),
        @ApiResponse(responseCode = "400", description = "Invalid token format",
            content = @Content(schema = @Schema(example = """
                {
                  "valid": false,
                  "error": "Token is required"
                }
                """)))
    })
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(
            @Parameter(description = "Token validation request", required = true,
                examples = @ExampleObject(value = """
                    {
                      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
                    }
                    """))
            @RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            if (token == null || token.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("valid", false);
                response.put("error", "Token is required");
                return ResponseEntity.badRequest().body(response);
            }

            boolean isValid = authService.validateToken(token);
            Map<String, Object> response = new HashMap<>();
            response.put("valid", isValid);

            if (isValid) {
                String username = authService.getUsernameFromToken(token);
                Set<String> roles = authService.getRolesFromToken(token);
                response.put("username", username);
                response.put("roles", roles);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Health Check", description = "Check if the authentication service is running")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Service is healthy",
            content = @Content(schema = @Schema(example = """
                {
                  "status": "UP",
                  "service": "auth-service"
                }
                """)))
    })
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "auth-service");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Logout", description = "Revoke a specific refresh token (logout from current device)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully logged out",
            content = @Content(schema = @Schema(example = """
                {
                  "message": "Successfully logged out"
                }
                """))),
        @ApiResponse(responseCode = "400", description = "Invalid refresh token",
            content = @Content(schema = @Schema(example = """
                {
                  "error": "Invalid refresh token"
                }
                """)))
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @Parameter(description = "Refresh token to revoke", required = true,
                examples = @ExampleObject(value = """
                    {
                      "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
                    }
                    """))
            @RequestBody Map<String, String> request) {
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Refresh token is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            authService.logout(refreshToken);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Successfully logged out");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid refresh token");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "Logout All Devices", description = "Revoke all refresh tokens for a user (logout from all devices)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully logged out from all devices",
            content = @Content(schema = @Schema(example = """
                {
                  "message": "Successfully logged out from all devices"
                }
                """))),
        @ApiResponse(responseCode = "400", description = "Invalid user ID",
            content = @Content(schema = @Schema(example = """
                {
                  "error": "Invalid user ID"
                }
                """)))
    })
    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(
            @Parameter(description = "User ID to logout all sessions", required = true,
                examples = @ExampleObject(value = """
                    {
                      "userId": 1
                    }
                    """))
            @RequestBody Map<String, Long> request) {
        try {
            Long userId = request.get("userId");
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User ID is required");
                return ResponseEntity.badRequest().body(error);
            }
            
            authService.logoutAll(userId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Successfully logged out from all devices");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid user ID");
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "Get Active Sessions", description = "Get all active sessions for a user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved active sessions",
            content = @Content(schema = @Schema(example = """
                {
                  "sessions": [
                    {
                      "token": "550e8400-e29b-41d4-a716-446655440000",
                      "deviceInfo": "Chrome on Windows (192.168.1.100)",
                      "createdAt": "2024-01-15T10:30:00Z",
                      "lastUsed": "2024-01-15T14:45:00Z"
                    }
                  ]
                }
                """))),
        @ApiResponse(responseCode = "400", description = "Invalid user ID",
            content = @Content(schema = @Schema(example = """
                {
                  "error": "Invalid user ID"
                }
                """)))
    })
    @GetMapping("/sessions/{userId}")
    public ResponseEntity<?> getActiveSessions(
            @Parameter(description = "User ID to get sessions for", required = true)
            @PathVariable Long userId) {
        try {
            List<Map<String, Object>> sessions = authService.getActiveSessions(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("sessions", sessions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid user ID");
            return ResponseEntity.badRequest().body(error);
        }
    }

    private String getDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        String remoteAddr = request.getRemoteAddr();
        
        StringBuilder deviceInfo = new StringBuilder();
        
        if (userAgent != null && !userAgent.isEmpty()) {
            // Extract browser and OS info from User-Agent
            if (userAgent.contains("Chrome")) {
                deviceInfo.append("Chrome");
            } else if (userAgent.contains("Firefox")) {
                deviceInfo.append("Firefox");
            } else if (userAgent.contains("Safari")) {
                deviceInfo.append("Safari");
            } else if (userAgent.contains("Edge")) {
                deviceInfo.append("Edge");
            } else {
                deviceInfo.append("Unknown Browser");
            }
            
            if (userAgent.contains("Windows")) {
                deviceInfo.append(" on Windows");
            } else if (userAgent.contains("Mac")) {
                deviceInfo.append(" on macOS");
            } else if (userAgent.contains("Linux")) {
                deviceInfo.append(" on Linux");
            } else if (userAgent.contains("Android")) {
                deviceInfo.append(" on Android");
            } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
                deviceInfo.append(" on iOS");
            }
        } else {
            deviceInfo.append("Unknown Device");
        }
        
        // Add IP address info
        String clientIp = xForwardedFor != null ? xForwardedFor.split(",")[0] : remoteAddr;
        if (clientIp != null && !clientIp.isEmpty()) {
            deviceInfo.append(" (").append(clientIp).append(")");
        }
        
        return deviceInfo.toString();
    }
} 