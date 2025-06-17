package com.microservices.ticketservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TicketCommentCreateDto {
    
    @NotBlank(message = "Content is required")
    private String content;
    
    @NotNull(message = "Ticket ID is required")
    private Long ticketId;

    // Constructors
    public TicketCommentCreateDto() {}

    public TicketCommentCreateDto(String content, Long ticketId) {
        this.content = content;
        this.ticketId = ticketId;
    }

    // Getters and Setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }
} 