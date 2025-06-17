package com.microservices.ticketservice.repository;

import com.microservices.ticketservice.entity.TicketComment;
import com.microservices.ticketservice.entity.Ticket;
import com.microservices.ticketservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {
    
    List<TicketComment> findByTicketOrderByCreatedAtDesc(Ticket ticket);
    
    Page<TicketComment> findByTicketOrderByCreatedAtDesc(Ticket ticket, Pageable pageable);
    
    List<TicketComment> findByAuthor(User author);
    
    Long countByTicket(Ticket ticket);
    
    @Query("SELECT c FROM TicketComment c WHERE c.ticket = :ticket AND c.content LIKE %:keyword%")
    Page<TicketComment> findByTicketAndContentContaining(@Param("ticket") Ticket ticket, 
                                                        @Param("keyword") String keyword, 
                                                        Pageable pageable);
} 