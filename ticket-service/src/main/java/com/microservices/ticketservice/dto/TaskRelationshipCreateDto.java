package com.microservices.ticketservice.dto;

import com.microservices.ticketservice.entity.TaskRelationshipType;
import jakarta.validation.constraints.NotNull;

public class TaskRelationshipCreateDto {
    
    @NotNull(message = "Source task ID is required")
    private Long sourceTaskId;
    
    @NotNull(message = "Target task ID is required")
    private Long targetTaskId;
    
    @NotNull(message = "Relationship type is required")
    private TaskRelationshipType relationshipType;
    
    private String description;

    // Constructors
    public TaskRelationshipCreateDto() {}

    public TaskRelationshipCreateDto(Long sourceTaskId, Long targetTaskId, TaskRelationshipType relationshipType) {
        this.sourceTaskId = sourceTaskId;
        this.targetTaskId = targetTaskId;
        this.relationshipType = relationshipType;
    }

    // Getters and Setters
    public Long getSourceTaskId() {
        return sourceTaskId;
    }

    public void setSourceTaskId(Long sourceTaskId) {
        this.sourceTaskId = sourceTaskId;
    }

    public Long getTargetTaskId() {
        return targetTaskId;
    }

    public void setTargetTaskId(Long targetTaskId) {
        this.targetTaskId = targetTaskId;
    }

    public TaskRelationshipType getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(TaskRelationshipType relationshipType) {
        this.relationshipType = relationshipType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
} 