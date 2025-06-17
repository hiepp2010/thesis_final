package com.microservices.auth.repository;

import com.microservices.auth.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    
    // Find refresh token by token string
    Optional<RefreshToken> findByToken(String token);
    
    // Find all refresh tokens for a user (for session management)
    List<RefreshToken> findByUserId(Long userId);
    
    // Find all refresh tokens for a username
    List<RefreshToken> findByUsername(String username);
    
    // Delete all refresh tokens for a user (logout from all devices)
    void deleteByUserId(Long userId);
    
    // Delete all refresh tokens for a username
    void deleteByUsername(String username);
    
    // Check if refresh token exists
    boolean existsByToken(String token);
} 