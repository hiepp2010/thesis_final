package com.microservices.ticketservice.service;

import com.microservices.ticketservice.dto.TaskRelationshipCreateDto;
import com.microservices.ticketservice.entity.TaskRelationship;
import com.microservices.ticketservice.entity.TaskRelationshipType;
import com.microservices.ticketservice.entity.Ticket;
import com.microservices.ticketservice.entity.User;
import com.microservices.ticketservice.repository.TaskRelationshipRepository;
import com.microservices.ticketservice.repository.TicketRepository;
import com.microservices.ticketservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TaskRelationshipService {

    @Autowired
    private TaskRelationshipRepository relationshipRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    public TaskRelationship createRelationship(TaskRelationshipCreateDto relationshipDto, Long createdBy) {
        // Validate that source and target tasks exist
        Ticket sourceTask = ticketRepository.findById(relationshipDto.getSourceTaskId())
                .orElseThrow(() -> new RuntimeException("Source task not found with id: " + relationshipDto.getSourceTaskId()));

        Ticket targetTask = ticketRepository.findById(relationshipDto.getTargetTaskId())
                .orElseThrow(() -> new RuntimeException("Target task not found with id: " + relationshipDto.getTargetTaskId()));

        User creator = userRepository.findById(createdBy)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + createdBy));

        // Prevent self-referencing relationships
        if (sourceTask.getId().equals(targetTask.getId())) {
            throw new RuntimeException("A task cannot have a relationship with itself");
        }

        // Check if relationship already exists
        Optional<TaskRelationship> existingRelationship = relationshipRepository
                .findBySourceTaskAndTargetTaskAndRelationshipType(sourceTask, targetTask, relationshipDto.getRelationshipType());
        
        if (existingRelationship.isPresent()) {
            throw new RuntimeException("This relationship already exists between these tasks");
        }

        // Check for circular dependencies for certain relationship types
        if (isCircularDependencyType(relationshipDto.getRelationshipType()) && 
            wouldCreateCircularDependency(sourceTask, targetTask, relationshipDto.getRelationshipType())) {
            throw new RuntimeException("Creating this relationship would create a circular dependency");
        }

        TaskRelationship relationship = TaskRelationship.builder()
                .sourceTask(sourceTask)
                .targetTask(targetTask)
                .relationshipType(relationshipDto.getRelationshipType())
                .description(relationshipDto.getDescription())
                .createdBy(creator)
                .build();

        return relationshipRepository.save(relationship);
    }

    public List<TaskRelationship> getRelationshipsByTask(Long taskId) {
        Ticket task = ticketRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
        
        return relationshipRepository.findAllRelationshipsByTask(task);
    }

    public List<TaskRelationship> getRelationshipsByProject(Long projectId) {
        return relationshipRepository.findAllRelationshipsByProject(projectId);
    }

    public List<TaskRelationship> getRelationshipsByType(TaskRelationshipType relationshipType) {
        return relationshipRepository.findByRelationshipType(relationshipType);
    }

    public boolean deleteRelationship(Long relationshipId, Long userId) {
        return relationshipRepository.findById(relationshipId).map(relationship -> {
            // Only creator or project members can delete relationships
            if (!relationship.getCreatedBy().getId().equals(userId)) {
                // Check if user is a member of either task's project
                if (!isUserProjectMember(userId, relationship.getSourceTask()) && 
                    !isUserProjectMember(userId, relationship.getTargetTask())) {
                    throw new RuntimeException("User does not have permission to delete this relationship");
                }
            }

            relationshipRepository.delete(relationship);
            return true;
        }).orElse(false);
    }

    public List<Ticket> getBlockedTasks() {
        return relationshipRepository.findBlockedTasks();
    }

    public List<Ticket> getBlockingTasks(Long taskId) {
        Ticket task = ticketRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
        
        return relationshipRepository.findBlockingTasks(task);
    }

    public List<Ticket> getSubtasks(Long parentTaskId) {
        Ticket parentTask = ticketRepository.findById(parentTaskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + parentTaskId));
        
        return relationshipRepository.findSubtasks(parentTask);
    }

    public List<Ticket> getParentTasks(Long subtaskId) {
        Ticket subtask = ticketRepository.findById(subtaskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + subtaskId));
        
        return relationshipRepository.findParentTasks(subtask);
    }

    public List<TaskRelationship> getOutgoingRelationships(Long taskId) {
        Ticket task = ticketRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
        
        return relationshipRepository.findBySourceTask(task);
    }

    public List<TaskRelationship> getIncomingRelationships(Long taskId) {
        Ticket task = ticketRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + taskId));
        
        return relationshipRepository.findByTargetTask(task);
    }

    private boolean isCircularDependencyType(TaskRelationshipType type) {
        return type == TaskRelationshipType.BLOCKED_BY || 
               type == TaskRelationshipType.DEPENDS_ON || 
               type == TaskRelationshipType.SUBTASK_OF ||
               type == TaskRelationshipType.FOLLOWS;
    }

    private boolean wouldCreateCircularDependency(Ticket sourceTask, Ticket targetTask, TaskRelationshipType type) {
        // For now, implement a simple check - in a production system, you'd want a more sophisticated algorithm
        return relationshipRepository.wouldCreateCircularDependency(sourceTask, targetTask);
    }

    private boolean isUserProjectMember(Long userId, Ticket task) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || task.getProject() == null) {
            return false;
        }

        return task.getProject().getOwner().getId().equals(userId) ||
               (task.getProject().getMembers() != null && task.getProject().getMembers().contains(user));
    }
} 