package com.microservices.ticketservice.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(redisHost);
        redisStandaloneConfiguration.setPort(redisPort);
        
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            redisStandaloneConfiguration.setPassword(redisPassword);
        }
        
        return new LettuceConnectionFactory(redisStandaloneConfiguration);
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // Configure JSON serializer
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        objectMapper.registerModule(new JavaTimeModule());
        
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // Default cache configuration
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15)) // Default 15 minutes TTL
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        // Specific cache configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // User data cache - 30 minutes (changes infrequently)
        cacheConfigurations.put("users", defaultCacheConfig.entryTtl(Duration.ofMinutes(30)));
        
        // Project data cache - 20 minutes (moderate frequency)
        cacheConfigurations.put("projects", defaultCacheConfig.entryTtl(Duration.ofMinutes(20)));
        
        // Project members cache - 15 minutes (for permission checks)
        cacheConfigurations.put("project-members", defaultCacheConfig.entryTtl(Duration.ofMinutes(15)));
        
        // User projects cache - 10 minutes (user's project list)
        cacheConfigurations.put("user-projects", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)));
        
        // Statistics cache - 5 minutes (updated frequently)
        cacheConfigurations.put("project-stats", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("ticket-counts", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Search results cache - 3 minutes (short-lived)
        cacheConfigurations.put("search-results", defaultCacheConfig.entryTtl(Duration.ofMinutes(3)));
        
        // Static data cache - 1 hour (rarely changes)
        cacheConfigurations.put("ticket-priorities", defaultCacheConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put("ticket-statuses", defaultCacheConfig.entryTtl(Duration.ofHours(1)));
        
        // JWT validation cache - 15 minutes (balance security/performance)
        cacheConfigurations.put("jwt-validation", defaultCacheConfig.entryTtl(Duration.ofMinutes(15)));
        
        // File metadata cache - 1 hour
        cacheConfigurations.put("file-metadata", defaultCacheConfig.entryTtl(Duration.ofHours(1)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
} 