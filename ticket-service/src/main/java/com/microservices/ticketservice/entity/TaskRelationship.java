package com.microservices.ticketservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "task_relationships", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"source_task_id", "target_task_id", "relationship_type"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskRelationship {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_task_id", nullable = false)
    @NotNull(message = "Source task is required")
    @ToString.Exclude
    private Ticket sourceTask;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_task_id", nullable = false)
    @NotNull(message = "Target task is required")
    @ToString.Exclude
    private Ticket targetTask;
    
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Relationship type is required")
    @Column(name = "relationship_type", nullable = false)
    private TaskRelationshipType relationshipType;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @NotNull(message = "Creator is required")
    @ToString.Exclude
    private User createdBy;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        // Prevent self-referencing relationships
        if (sourceTask != null && targetTask != null && sourceTask.getId().equals(targetTask.getId())) {
            throw new IllegalArgumentException("A task cannot have a relationship with itself");
        }
    }
    
    /**
     * Get a human-readable description of this relationship
     */
    public String getReadableDescription() {
        if (sourceTask != null && targetTask != null && relationshipType != null) {
            return relationshipType.getReadableFormat(sourceTask.getTitle(), targetTask.getTitle());
        }
        return "Unknown relationship";
    }
} 