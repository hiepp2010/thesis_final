package com.microservices.ticketservice.repository;

import com.microservices.ticketservice.entity.Ticket;
import com.microservices.ticketservice.entity.TicketStatus;
import com.microservices.ticketservice.entity.TicketPriority;
import com.microservices.ticketservice.entity.Project;
import com.microservices.ticketservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
    List<Ticket> findByProject(Project project);
    
    List<Ticket> findByAssignee(User assignee);
    
    List<Ticket> findByStatus(TicketStatus status);
    
    List<Ticket> findByPriority(TicketPriority priority);
    
    Page<Ticket> findByProject(Project project, Pageable pageable);
    
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);
    
    Page<Ticket> findByAssignee(User assignee, Pageable pageable);
    
    Page<Ticket> findByProjectAndStatus(Project project, TicketStatus status, Pageable pageable);
    
    @Query("SELECT t FROM Ticket t WHERE t.title LIKE %:keyword% OR t.description LIKE %:keyword%")
    Page<Ticket> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = :status")
    Long countByStatus(@Param("status") TicketStatus status);
    
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignee = :assignee AND t.status != 'CLOSED'")
    Long countOpenTicketsByAssignee(@Param("assignee") User assignee);
    
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.project = :project")
    Long countByProject(@Param("project") Project project);
    
    @Query("SELECT AVG(t.finishPercentage) FROM Ticket t WHERE t.project = :project")
    Double getAverageProgressByProject(@Param("project") Project project);
} 