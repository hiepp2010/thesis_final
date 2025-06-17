package com.microservices.auth.service;

import com.microservices.auth.entity.RefreshToken;
import com.microservices.auth.repository.RefreshTokenRepository;
import com.microservices.auth.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${jwt.refresh-token-expiration:604800000}")
    private Long refreshTokenExpiration;

    /**
     * Create and store a new refresh token
     */
    public String createRefreshToken(Long userId, String username, String deviceInfo) {
        try {
            // Generate unique token ID (not JWT for better control)
            String tokenId = UUID.randomUUID().toString();
            
            // Create RefreshToken entity
            RefreshToken refreshToken = new RefreshToken(tokenId, userId, username, deviceInfo);
            
            // Save to Redis (automatically expires after TTL)
            refreshTokenRepository.save(refreshToken);
            
            logger.info("Created refresh token for user: {} with token ID: {}", username, tokenId);
            return tokenId;
            
        } catch (Exception e) {
            logger.error("Failed to create refresh token for user: {}", username, e);
            throw new RuntimeException("Failed to create refresh token");
        }
    }

    /**
     * Validate refresh token and return user info
     */
    public Optional<RefreshToken> validateRefreshToken(String token) {
        try {
            Optional<RefreshToken> refreshTokenOpt = refreshTokenRepository.findById(token);
            
            if (refreshTokenOpt.isPresent()) {
                RefreshToken refreshToken = refreshTokenOpt.get();
                
                // Update last used timestamp
                refreshToken.updateLastUsed();
                refreshTokenRepository.save(refreshToken);
                
                logger.debug("Validated refresh token for user: {}", refreshToken.getUsername());
                return refreshTokenOpt;
            } else {
                logger.warn("Invalid refresh token provided: {}", token);
                return Optional.empty();
            }
            
        } catch (Exception e) {
            logger.error("Error validating refresh token: {}", token, e);
            return Optional.empty();
        }
    }

    /**
     * Revoke a specific refresh token
     */
    public boolean revokeRefreshToken(String token) {
        try {
            if (refreshTokenRepository.existsById(token)) {
                refreshTokenRepository.deleteById(token);
                logger.info("Revoked refresh token: {}", token);
                return true;
            } else {
                logger.warn("Attempted to revoke non-existent token: {}", token);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error revoking refresh token: {}", token, e);
            return false;
        }
    }

    /**
     * Revoke all refresh tokens for a user (logout from all devices)
     */
    public void revokeAllUserTokens(Long userId) {
        try {
            List<RefreshToken> userTokens = refreshTokenRepository.findByUserId(userId);
            for (RefreshToken token : userTokens) {
                refreshTokenRepository.deleteById(token.getToken());
            }
            logger.info("Revoked {} refresh tokens for user ID: {}", userTokens.size(), userId);
        } catch (Exception e) {
            logger.error("Error revoking all tokens for user ID: {}", userId, e);
        }
    }

    /**
     * Revoke all refresh tokens for a username
     */
    public void revokeAllUserTokens(String username) {
        try {
            List<RefreshToken> userTokens = refreshTokenRepository.findByUsername(username);
            for (RefreshToken token : userTokens) {
                refreshTokenRepository.deleteById(token.getToken());
            }
            logger.info("Revoked {} refresh tokens for username: {}", userTokens.size(), username);
        } catch (Exception e) {
            logger.error("Error revoking all tokens for username: {}", username, e);
        }
    }

    /**
     * Get all active sessions for a user
     */
    public List<RefreshToken> getUserActiveSessions(Long userId) {
        return refreshTokenRepository.findByUserId(userId);
    }

    /**
     * Get all active sessions for a username
     */
    public List<RefreshToken> getUserActiveSessions(String username) {
        return refreshTokenRepository.findByUsername(username);
    }

    /**
     * Check if refresh token exists
     */
    public boolean isRefreshTokenValid(String token) {
        return refreshTokenRepository.existsById(token);
    }
} 