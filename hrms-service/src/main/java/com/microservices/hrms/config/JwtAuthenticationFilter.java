package com.microservices.hrms.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        // Extract user information from headers set by API Gateway
        String userId = request.getHeader("X-User-Id");
        String username = request.getHeader("X-Username");
        String userRoles = request.getHeader("X-User-Roles");
        String userEmail = request.getHeader("X-User-Email");
        
        if (userId != null && username != null) {
            // Parse roles
            List<SimpleGrantedAuthority> authorities = Arrays.stream(userRoles.split(","))
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.trim().toUpperCase()))
                    .collect(Collectors.toList());
            
            // Create authentication token
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(username, null, authorities);
            
            // Add user details to authentication
            UserPrincipal userPrincipal = UserPrincipal.builder()
                    .userId(Long.parseLong(userId))
                    .username(username)
                    .email(userEmail)
                    .roles(userRoles)
                    .build();
            
            authentication.setDetails(userPrincipal);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        filterChain.doFilter(request, response);
    }
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") || path.equals("/api/hrms/health");
    }
} 