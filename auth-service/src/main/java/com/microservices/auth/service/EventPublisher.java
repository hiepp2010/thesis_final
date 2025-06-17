package com.microservices.auth.service;

import com.microservices.auth.event.UserRegisteredEvent;
import com.microservices.auth.event.UserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(EventPublisher.class);

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.user-events:user-events}")
    private String userEventsTopic;

    public void publishUserRegisteredEvent(UserRegisteredEvent event) {
        try {
            logger.info("Publishing user registered event: {}", event);
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(userEventsTopic, event.getUserId().toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to publish user registered event: {}", event, ex);
                } else {
                    logger.info("Successfully published user registered event for userId: {} to topic: {}", 
                              event.getUserId(), userEventsTopic);
                }
            });
            
        } catch (Exception e) {
            logger.error("Error publishing user registered event: {}", event, e);
        }
    }

    public void publishUserEvent(UserEvent event) {
        try {
            logger.info("Publishing user event: {}", event);
            
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(userEventsTopic, event.getUserId().toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to publish user event: {}", event, ex);
                } else {
                    logger.info("Successfully published user event for userId: {} to topic: {}", 
                              event.getUserId(), userEventsTopic);
                }
            });
            
        } catch (Exception e) {
            logger.error("Error publishing user event: {}", event, e);
        }
    }
} 