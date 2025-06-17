package com.microservices.ticketservice.controller;

import com.microservices.ticketservice.config.HeaderAuthenticationFilter;
import com.microservices.ticketservice.dto.TaskRelationshipCreateDto;
import com.microservices.ticketservice.entity.TaskRelationship;
import com.microservices.ticketservice.entity.TaskRelationshipType;
import com.microservices.ticketservice.entity.Ticket;
import com.microservices.ticketservice.service.TaskRelationshipService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/relationships")
@CrossOrigin(origins = "*")
public class TaskRelationshipController {

    @Autowired
    private TaskRelationshipService relationshipService;

    @PostMapping
    public ResponseEntity<TaskRelationship> createRelationship(@Valid @RequestBody TaskRelationshipCreateDto relationshipDto) {
        Long userId = getCurrentUserId();
        TaskRelationship createdRelationship = relationshipService.createRelationship(relationshipDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRelationship);
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<List<TaskRelationship>> getRelationshipsByTask(@PathVariable Long taskId) {
        List<TaskRelationship> relationships = relationshipService.getRelationshipsByTask(taskId);
        return ResponseEntity.ok(relationships);
    }

    @GetMapping("/task/{taskId}/outgoing")
    public ResponseEntity<List<TaskRelationship>> getOutgoingRelationships(@PathVariable Long taskId) {
        List<TaskRelationship> relationships = relationshipService.getOutgoingRelationships(taskId);
        return ResponseEntity.ok(relationships);
    }

    @GetMapping("/task/{taskId}/incoming")
    public ResponseEntity<List<TaskRelationship>> getIncomingRelationships(@PathVariable Long taskId) {
        List<TaskRelationship> relationships = relationshipService.getIncomingRelationships(taskId);
        return ResponseEntity.ok(relationships);
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskRelationship>> getRelationshipsByProject(@PathVariable Long projectId) {
        List<TaskRelationship> relationships = relationshipService.getRelationshipsByProject(projectId);
        return ResponseEntity.ok(relationships);
    }

    @GetMapping("/type/{relationshipType}")
    public ResponseEntity<List<TaskRelationship>> getRelationshipsByType(@PathVariable TaskRelationshipType relationshipType) {
        List<TaskRelationship> relationships = relationshipService.getRelationshipsByType(relationshipType);
        return ResponseEntity.ok(relationships);
    }

    @DeleteMapping("/{relationshipId}")
    public ResponseEntity<Void> deleteRelationship(@PathVariable Long relationshipId) {
        Long userId = getCurrentUserId();
        boolean deleted = relationshipService.deleteRelationship(relationshipId, userId);
        return deleted ? ResponseEntity.noContent().build() 
                      : ResponseEntity.notFound().build();
    }

    @GetMapping("/blocked-tasks")
    public ResponseEntity<List<Ticket>> getBlockedTasks() {
        List<Ticket> blockedTasks = relationshipService.getBlockedTasks();
        return ResponseEntity.ok(blockedTasks);
    }

    @GetMapping("/task/{taskId}/blocking")
    public ResponseEntity<List<Ticket>> getBlockingTasks(@PathVariable Long taskId) {
        List<Ticket> blockingTasks = relationshipService.getBlockingTasks(taskId);
        return ResponseEntity.ok(blockingTasks);
    }

    @GetMapping("/task/{taskId}/subtasks")
    public ResponseEntity<List<Ticket>> getSubtasks(@PathVariable Long taskId) {
        List<Ticket> subtasks = relationshipService.getSubtasks(taskId);
        return ResponseEntity.ok(subtasks);
    }

    @GetMapping("/task/{taskId}/parents")
    public ResponseEntity<List<Ticket>> getParentTasks(@PathVariable Long taskId) {
        List<Ticket> parentTasks = relationshipService.getParentTasks(taskId);
        return ResponseEntity.ok(parentTasks);
    }

    @GetMapping("/types")
    public ResponseEntity<TaskRelationshipType[]> getRelationshipTypes() {
        return ResponseEntity.ok(TaskRelationshipType.values());
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof HeaderAuthenticationFilter.UserAuthentication) {
            HeaderAuthenticationFilter.UserAuthentication userAuth = 
                (HeaderAuthenticationFilter.UserAuthentication) authentication;
            return userAuth.getUserIdAsLong();
        }
        return 1L; // Default fallback
    }
} 