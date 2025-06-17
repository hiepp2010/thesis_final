package com.microservices.ticketservice.entity;

public enum TaskRelationshipType {
    BLOCKED_BY("is blocked by"),
    BLOCKS("blocks"),
    DEPENDS_ON("depends on"),
    RELATED_TO("is related to"),
    DUPLICATE_OF("is duplicate of"),
    SUBTASK_OF("is subtask of"),
    PARENT_OF("is parent of"),
    FOLLOWS("follows"),
    PRECEDES("precedes");
    
    private final String description;
    
    TaskRelationshipType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getReadableFormat(String sourceTaskTitle, String targetTaskTitle) {
        return sourceTaskTitle + " " + description + " " + targetTaskTitle;
    }
} 