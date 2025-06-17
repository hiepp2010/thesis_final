package com.microservices.ticketservice.repository;

import com.microservices.ticketservice.entity.Attachment;
import com.microservices.ticketservice.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    
    List<Attachment> findByTicket(Ticket ticket);
    
    List<Attachment> findByTicketId(Long ticketId);
    
    List<Attachment> findByUploadedBy(Long uploadedBy);
    
    void deleteByTicket(Ticket ticket);
} 