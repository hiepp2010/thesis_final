package com.microservices.ticketservice.repository;

import com.microservices.ticketservice.entity.TaskRelationship;
import com.microservices.ticketservice.entity.TaskRelationshipType;
import com.microservices.ticketservice.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRelationshipRepository extends JpaRepository<TaskRelationship, Long> {
    
    List<TaskRelationship> findBySourceTask(Ticket sourceTask);
    
    List<TaskRelationship> findByTargetTask(Ticket targetTask);
    
    List<TaskRelationship> findByRelationshipType(TaskRelationshipType relationshipType);
    
    List<TaskRelationship> findBySourceTaskAndRelationshipType(Ticket sourceTask, TaskRelationshipType relationshipType);
    
    List<TaskRelationship> findByTargetTaskAndRelationshipType(Ticket targetTask, TaskRelationshipType relationshipType);
    
    Optional<TaskRelationship> findBySourceTaskAndTargetTaskAndRelationshipType(
        Ticket sourceTask, Ticket targetTask, TaskRelationshipType relationshipType);
    
    @Query("SELECT tr FROM TaskRelationship tr WHERE tr.sourceTask = :task OR tr.targetTask = :task")
    List<TaskRelationship> findAllRelationshipsByTask(@Param("task") Ticket task);
    
    @Query("SELECT tr FROM TaskRelationship tr WHERE tr.sourceTask.project.id = :projectId OR tr.targetTask.project.id = :projectId")
    List<TaskRelationship> findAllRelationshipsByProject(@Param("projectId") Long projectId);
    
    // Find blocked tasks
    @Query("SELECT tr.sourceTask FROM TaskRelationship tr WHERE tr.relationshipType = 'BLOCKED_BY' AND tr.targetTask.status != 'CLOSED'")
    List<Ticket> findBlockedTasks();
    
    // Find blocking tasks for a specific task
    @Query("SELECT tr.targetTask FROM TaskRelationship tr WHERE tr.relationshipType = 'BLOCKED_BY' AND tr.sourceTask = :task")
    List<Ticket> findBlockingTasks(@Param("task") Ticket task);
    
    // Check if creating this relationship would create a circular dependency
    @Query("SELECT CASE WHEN COUNT(tr) > 0 THEN true ELSE false END FROM TaskRelationship tr " +
           "WHERE tr.sourceTask = :targetTask AND tr.targetTask = :sourceTask")
    boolean wouldCreateCircularDependency(@Param("sourceTask") Ticket sourceTask, @Param("targetTask") Ticket targetTask);
    
    // Find subtasks
    @Query("SELECT tr.sourceTask FROM TaskRelationship tr WHERE tr.relationshipType = 'SUBTASK_OF' AND tr.targetTask = :parentTask")
    List<Ticket> findSubtasks(@Param("parentTask") Ticket parentTask);
    
    // Find parent tasks
    @Query("SELECT tr.targetTask FROM TaskRelationship tr WHERE tr.relationshipType = 'SUBTASK_OF' AND tr.sourceTask = :subtask")
    List<Ticket> findParentTasks(@Param("subtask") Ticket subtask);
} 