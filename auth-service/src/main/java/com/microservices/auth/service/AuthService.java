package com.microservices.auth.service;

import com.microservices.auth.dto.AuthResponse;
import com.microservices.auth.dto.LoginRequest;
import com.microservices.auth.dto.RegisterRequest;
import com.microservices.auth.entity.RefreshToken;
import com.microservices.auth.entity.Role;
import com.microservices.auth.entity.User;
import com.microservices.auth.event.UserRegisteredEvent;
import com.microservices.auth.event.UserEvent;
import com.microservices.auth.repository.RoleRepository;
import com.microservices.auth.repository.UserRepository;
import com.microservices.auth.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private RefreshTokenService refreshTokenService;

    public AuthResponse login(LoginRequest loginRequest, String deviceInfo) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (!user.getIsActive()) {
            throw new RuntimeException("User account is disabled");
        }

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        String accessToken = jwtUtils.generateAccessToken(user.getUsername(), user.getId(), roleNames, user.getEmail());
        String refreshToken = refreshTokenService.createRefreshToken(user.getId(), user.getUsername(), deviceInfo != null ? deviceInfo : "Unknown Device");

        return new AuthResponse(accessToken, refreshToken, user.getId(), user.getUsername(), user.getEmail(), roleNames);
    }

    public AuthResponse register(RegisterRequest registerRequest, String deviceInfo) {
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());

        // Assign default USER role
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new RuntimeException("Default role not found"));
        
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        User savedUser = userRepository.save(user);

        Set<String> roleNames = savedUser.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        // Publish user registered event to Kafka
        try {
            // Create the fullName from firstName and lastName
            String fullName = (savedUser.getFirstName() != null ? savedUser.getFirstName() : "") + 
                              " " + 
                              (savedUser.getLastName() != null ? savedUser.getLastName() : "");
            fullName = fullName.trim();

            UserEvent event = new UserEvent(
                "USER_CREATED",
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                fullName
            );
            eventPublisher.publishUserEvent(event);
            logger.info("Published USER_CREATED event for user: {}", savedUser.getUsername());
        } catch (Exception e) {
            logger.error("Failed to publish USER_CREATED event for user: {}", savedUser.getUsername(), e);
            // Don't fail the registration if event publishing fails
        }

        String accessToken = jwtUtils.generateAccessToken(savedUser.getUsername(), savedUser.getId(), roleNames, savedUser.getEmail());
        String refreshToken = refreshTokenService.createRefreshToken(savedUser.getId(), savedUser.getUsername(), deviceInfo != null ? deviceInfo : "Unknown Device");

        return new AuthResponse(accessToken, refreshToken, savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(), roleNames);
    }

    public AuthResponse refreshToken(String refreshToken) {
        // Validate refresh token using Redis
        Optional<RefreshToken> refreshTokenOpt = refreshTokenService.validateRefreshToken(refreshToken);
        if (refreshTokenOpt.isEmpty()) {
            throw new RuntimeException("Invalid refresh token");
        }

        RefreshToken tokenData = refreshTokenOpt.get();
        String username = tokenData.getUsername();
        Long userId = tokenData.getUserId();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getIsActive()) {
            throw new RuntimeException("User account is disabled");
        }

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        // Generate new access token
        String newAccessToken = jwtUtils.generateAccessToken(username, userId, roleNames, user.getEmail());
        
        // Create new refresh token and revoke old one
        refreshTokenService.revokeRefreshToken(refreshToken);
        String newRefreshToken = refreshTokenService.createRefreshToken(userId, username, tokenData.getDeviceInfo());

        return new AuthResponse(newAccessToken, newRefreshToken, userId, username, user.getEmail(), roleNames);
    }

    public void logout(String refreshToken) {
        boolean revoked = refreshTokenService.revokeRefreshToken(refreshToken);
        if (!revoked) {
            throw new RuntimeException("Refresh token not found or already revoked");
        }
    }

    public void logoutAll(Long userId) {
        refreshTokenService.revokeAllUserTokens(userId);
    }

    public List<Map<String, Object>> getActiveSessions(Long userId) {
        List<RefreshToken> tokens = refreshTokenService.getUserActiveSessions(userId);
        return tokens.stream().map(token -> {
            Map<String, Object> session = new HashMap<>();
            session.put("token", token.getToken());
            session.put("deviceInfo", token.getDeviceInfo());
            session.put("createdAt", token.getCreatedAt());
            session.put("lastUsed", token.getLastUsedAt());
            return session;
        }).collect(Collectors.toList());
    }

    public boolean validateToken(String token) {
        return jwtUtils.validateToken(token);
    }

    public String getUsernameFromToken(String token) {
        return jwtUtils.getUsernameFromToken(token);
    }

    public Set<String> getRolesFromToken(String token) {
        return jwtUtils.getRolesFromToken(token);
    }
} 