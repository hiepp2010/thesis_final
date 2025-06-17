package com.microservices.ticketservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProjectCommentCreateDto {
    
    @NotBlank(message = "Comment content is required")
    private String content;
    
    @NotNull(message = "Project ID is required")
    private Long projectId;

    // Constructors
    public ProjectCommentCreateDto() {}

    public ProjectCommentCreateDto(String content, Long projectId) {
        this.content = content;
        this.projectId = projectId;
    }

    // Getters and Setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
} 