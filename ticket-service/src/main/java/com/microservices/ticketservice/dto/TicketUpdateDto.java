package com.microservices.ticketservice.dto;

import com.microservices.ticketservice.entity.TicketPriority;
import com.microservices.ticketservice.entity.TicketStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;

public class TicketUpdateDto {
    
    private String title;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private Long assigneeId;
    private LocalDateTime dueDate;
    
    @Min(value = 0, message = "Finish percentage must be between 0 and 100")
    @Max(value = 100, message = "Finish percentage must be between 0 and 100")
    private Integer finishPercentage;

    // Constructors
    public TicketUpdateDto() {}

    // Getters and Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    public Integer getFinishPercentage() {
        return finishPercentage;
    }

    public void setFinishPercentage(Integer finishPercentage) {
        this.finishPercentage = finishPercentage;
    }
} 