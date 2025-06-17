package com.microservices.ticketservice.service;

import com.microservices.ticketservice.event.UserEvent;
import com.microservices.ticketservice.entity.User;
import com.microservices.ticketservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumerService {

    private final UserRepository userRepository;

    @KafkaListener(topics = "user-events", groupId = "ticket-service-group-v2")
    @Transactional
    public void handleUserEvent(UserEvent userEvent) {
        log.info("Received user event: {}", userEvent);

        try {
            if ("USER_CREATED".equals(userEvent.getEventType())) {
                handleUserCreated(userEvent);
            } else if ("USER_UPDATED".equals(userEvent.getEventType())) {
                handleUserUpdated(userEvent);
            } else if ("USER_DELETED".equals(userEvent.getEventType())) {
                handleUserDeleted(userEvent);
            } else {
                log.warn("Unknown user event type: {}", userEvent.getEventType());
            }
        } catch (Exception e) {
            log.error("Failed to process user event: {}", userEvent, e);
            throw e;
        }
    }

    private void handleUserCreated(UserEvent userEvent) {
        if (userRepository.existsById(userEvent.getUserId())) {
            log.warn("User with ID {} already exists, skipping creation", userEvent.getUserId());
            return;
        }

        String fullName = (userEvent.getFullName() != null && !userEvent.getFullName().trim().isEmpty()) 
                ? userEvent.getFullName() 
                : userEvent.getUsername();
        
        User user = User.builder()
                .id(userEvent.getUserId())
                .username(userEvent.getUsername())
                .email(userEvent.getEmail())
                .fullName(fullName)
                .build();

        userRepository.save(user);
        log.info("Created new user: {}", user.getUsername());
    }

    private void handleUserUpdated(UserEvent userEvent) {
        User user = userRepository.findById(userEvent.getUserId()).orElse(null);
        if (user == null) {
            log.warn("User with ID {} not found for update, creating new user", userEvent.getUserId());
            handleUserCreated(userEvent);
            return;
        }

        String fullName = (userEvent.getFullName() != null && !userEvent.getFullName().trim().isEmpty()) 
                ? userEvent.getFullName() 
                : userEvent.getUsername();
        
        user.setUsername(userEvent.getUsername());
        user.setEmail(userEvent.getEmail());
        user.setFullName(fullName);

        userRepository.save(user);
        log.info("Updated user: {}", user.getUsername());
    }

    private void handleUserDeleted(UserEvent userEvent) {
        if (userRepository.existsById(userEvent.getUserId())) {
            userRepository.deleteById(userEvent.getUserId());
            log.info("Deleted user with ID: {}", userEvent.getUserId());
        } else {
            log.warn("User with ID {} not found for deletion", userEvent.getUserId());
        }
    }
} 