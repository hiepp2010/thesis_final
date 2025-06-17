package com.microservices.ticketservice.controller;

import com.microservices.ticketservice.config.HeaderAuthenticationFilter;
import com.microservices.ticketservice.dto.TicketCommentCreateDto;
import com.microservices.ticketservice.entity.TicketComment;
import com.microservices.ticketservice.service.TicketCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
@Tag(name = "Ticket Comments", description = "Ticket comment management APIs")
public class TicketCommentController {

    @Autowired
    private TicketCommentService commentService;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof HeaderAuthenticationFilter.UserAuthentication) {
            HeaderAuthenticationFilter.UserAuthentication userAuth = 
                (HeaderAuthenticationFilter.UserAuthentication) authentication;
            return userAuth.getUserIdAsLong();
        }
        return 1L; // Default fallback
    }

    @Operation(summary = "Get comments for a ticket", description = "Retrieve all comments for a specific ticket")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Comments retrieved successfully"),
        @ApiResponse(responseCode = "403", description = "Access denied - not a project member"),
        @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<List<TicketComment>> getTicketComments(@PathVariable Long ticketId) {
        Long userId = getCurrentUserId();
        List<TicketComment> comments = commentService.getCommentsByTicket(ticketId, userId);
        return ResponseEntity.ok(comments);
    }

    @Operation(summary = "Get paginated comments for a ticket", description = "Retrieve paginated comments for a specific ticket")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/paginated")
    public ResponseEntity<Page<TicketComment>> getTicketCommentsPaginated(
            @PathVariable Long ticketId,
            Pageable pageable) {
        Long userId = getCurrentUserId();
        Page<TicketComment> comments = commentService.getCommentsByTicket(ticketId, userId, pageable);
        return ResponseEntity.ok(comments);
    }

    @Operation(summary = "Create a comment", description = "Add a new comment to a ticket")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Comment created successfully",
            content = @Content(schema = @Schema(implementation = TicketComment.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "403", description = "Access denied - not a project member"),
        @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<TicketComment> createComment(
            @PathVariable Long ticketId,
            @Parameter(description = "Comment content", required = true)
            @Valid @RequestBody TicketCommentCreateDto commentDto) {
        
        // Ensure the ticketId in path matches the DTO
        commentDto.setTicketId(ticketId);
        
        Long userId = getCurrentUserId();
        TicketComment createdComment = commentService.createComment(commentDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
    }

    @Operation(summary = "Update a comment", description = "Update the content of an existing comment")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{commentId}")
    public ResponseEntity<TicketComment> updateComment(
            @PathVariable Long ticketId,
            @PathVariable Long commentId,
            @RequestBody Map<String, String> updateRequest) {
        
        Long userId = getCurrentUserId();
        String newContent = updateRequest.get("content");
        
        if (newContent == null || newContent.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        Optional<TicketComment> updatedComment = commentService.updateComment(commentId, newContent, userId);
        return updatedComment.map(ResponseEntity::ok)
                           .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete a comment", description = "Delete a comment from a ticket")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long ticketId,
            @PathVariable Long commentId) {
        
        Long userId = getCurrentUserId();
        boolean deleted = commentService.deleteComment(commentId, userId);
        return deleted ? ResponseEntity.noContent().build() 
                      : ResponseEntity.notFound().build();
    }

    @Operation(summary = "Get comment count", description = "Get the total number of comments for a ticket")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCommentCount(@PathVariable Long ticketId) {
        Long count = commentService.getCommentCountByTicket(ticketId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @Operation(summary = "Search comments", description = "Search comments in a ticket by keyword")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/search")
    public ResponseEntity<Page<TicketComment>> searchComments(
            @PathVariable Long ticketId,
            @RequestParam String keyword,
            Pageable pageable) {
        
        Long userId = getCurrentUserId();
        Page<TicketComment> comments = commentService.searchCommentsInTicket(ticketId, keyword, userId, pageable);
        return ResponseEntity.ok(comments);
    }
} 