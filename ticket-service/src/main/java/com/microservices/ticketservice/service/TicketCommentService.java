package com.microservices.ticketservice.service;

import com.microservices.ticketservice.dto.TicketCommentCreateDto;
import com.microservices.ticketservice.entity.TicketComment;
import com.microservices.ticketservice.entity.Ticket;
import com.microservices.ticketservice.entity.User;
import com.microservices.ticketservice.repository.TicketCommentRepository;
import com.microservices.ticketservice.repository.TicketRepository;
import com.microservices.ticketservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TicketCommentService {

    @Autowired
    private TicketCommentRepository commentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectService projectService;

    public List<TicketComment> getCommentsByTicket(Long ticketId, Long userId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + ticketId));

        // Check if user is project member to view comments
        if (!projectService.isUserProjectMember(userId, ticket.getProject().getId())) {
            throw new RuntimeException("Access denied. Only project members can view ticket comments.");
        }

        return commentRepository.findByTicketOrderByCreatedAtDesc(ticket);
    }

    public Page<TicketComment> getCommentsByTicket(Long ticketId, Long userId, Pageable pageable) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + ticketId));

        // Check if user is project member to view comments
        if (!projectService.isUserProjectMember(userId, ticket.getProject().getId())) {
            throw new RuntimeException("Access denied. Only project members can view ticket comments.");
        }

        return commentRepository.findByTicketOrderByCreatedAtDesc(ticket, pageable);
    }

    public TicketComment createComment(TicketCommentCreateDto commentDto, Long authorId) {
        // Get ticket
        Ticket ticket = ticketRepository.findById(commentDto.getTicketId())
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + commentDto.getTicketId()));

        // Get author
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + authorId));

        // Check if user is project member - only project members can comment
        if (!projectService.isUserProjectMember(authorId, ticket.getProject().getId())) {
            throw new RuntimeException("Access denied. Only project members can comment on tickets.");
        }

        TicketComment comment = TicketComment.builder()
                .content(commentDto.getContent())
                .ticket(ticket)
                .author(author)
                .build();

        return commentRepository.save(comment);
    }

    public Optional<TicketComment> updateComment(Long commentId, String newContent, Long userId) {
        return commentRepository.findById(commentId).map(comment -> {
            // Check if user is the author of the comment
            if (!comment.getAuthor().getId().equals(userId)) {
                throw new RuntimeException("Access denied. Users can only edit their own comments.");
            }

            comment.setContent(newContent);
            return commentRepository.save(comment);
        });
    }

    public boolean deleteComment(Long commentId, Long userId) {
        return commentRepository.findById(commentId).map(comment -> {
            // Check if user is the author or project owner
            if (!comment.getAuthor().getId().equals(userId) && 
                !projectService.isUserProjectOwner(userId, comment.getTicket().getProject().getId())) {
                throw new RuntimeException("Access denied. Users can only delete their own comments or project owner can delete any comment.");
            }

            commentRepository.delete(comment);
            return true;
        }).orElse(false);
    }

    public List<TicketComment> getCommentsByAuthor(Long authorId) {
        Optional<User> author = userRepository.findById(authorId);
        return author.map(commentRepository::findByAuthor).orElse(List.of());
    }

    public Long getCommentCountByTicket(Long ticketId) {
        Optional<Ticket> ticket = ticketRepository.findById(ticketId);
        return ticket.map(commentRepository::countByTicket).orElse(0L);
    }

    public Page<TicketComment> searchCommentsInTicket(Long ticketId, String keyword, Long userId, Pageable pageable) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + ticketId));

        // Check if user is project member to search comments
        if (!projectService.isUserProjectMember(userId, ticket.getProject().getId())) {
            throw new RuntimeException("Access denied. Only project members can search ticket comments.");
        }

        return commentRepository.findByTicketAndContentContaining(ticket, keyword, pageable);
    }
} 