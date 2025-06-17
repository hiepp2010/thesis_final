package com.microservices.ticketservice.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String username = request.getHeader("X-Username");
        String userEmail = request.getHeader("X-User-Email");
        String userRoles = request.getHeader("X-User-Roles");

        // Add debug logging
        System.out.println("HeaderAuthenticationFilter called for: " + request.getRequestURI());
        System.out.println("X-User-Id: " + userId);
        System.out.println("X-Username: " + username);
        System.out.println("X-User-Email: " + userEmail);
        System.out.println("X-User-Roles: " + userRoles);

        if (userId != null && username != null) {
            List<SimpleGrantedAuthority> authorities = parseRoles(userRoles);

            // Create a custom authentication token that includes user info
            UserAuthentication auth = new UserAuthentication(username, authorities, userId, userEmail);
            SecurityContextHolder.getContext().setAuthentication(auth);
            System.out.println("Authentication set for user: " + username + " (ID: " + userId + ")");
        } else {
            System.out.println("No authentication headers found - userId: " + userId + ", username: " + username);
        }

        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> parseRoles(String roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
        
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    // Custom authentication class to hold user information
    public static class UserAuthentication extends UsernamePasswordAuthenticationToken {
        private final String userId;
        private final String email;

        public UserAuthentication(String username, List<SimpleGrantedAuthority> authorities, 
                                String userId, String email) {
            super(username, null, authorities);
            this.userId = userId;
            this.email = email;
        }

        public String getUserId() {
            return userId;
        }

        public String getEmail() {
            return email;
        }

        public Long getUserIdAsLong() {
            try {
                return userId != null ? Long.parseLong(userId) : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }
} 