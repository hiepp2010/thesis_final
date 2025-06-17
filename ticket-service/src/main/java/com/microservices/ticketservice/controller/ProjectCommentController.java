package com.microservices.ticketservice.controller;

import com.microservices.ticketservice.config.HeaderAuthenticationFilter;
import com.microservices.ticketservice.dto.ProjectCommentCreateDto;
import com.microservices.ticketservice.entity.ProjectComment;
import com.microservices.ticketservice.service.ProjectCommentService;
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
@RequestMapping("/api/projects/comments")
@CrossOrigin(origins = "*")
public class ProjectCommentController {

    @Autowired
    private ProjectCommentService commentService;

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ProjectComment>> getCommentsByProject(@PathVariable Long projectId) {
        List<ProjectComment> comments = commentService.getCommentsByProject(projectId);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/project/{projectId}/paginated")
    public ResponseEntity<Page<ProjectComment>> getCommentsByProjectPaginated(
            @PathVariable Long projectId, Pageable pageable) {
        Page<ProjectComment> comments = commentService.getCommentsByProject(projectId, pageable);
        return ResponseEntity.ok(comments);
    }

    @PostMapping
    public ResponseEntity<ProjectComment> createComment(@Valid @RequestBody ProjectCommentCreateDto commentDto) {
        Long userId = getCurrentUserId();
        ProjectComment createdComment = commentService.createComment(commentDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<ProjectComment> updateComment(
            @PathVariable Long commentId,
            @RequestBody Map<String, String> request) {
        
        Long userId = getCurrentUserId();
        String newContent = request.get("content");
        
        if (newContent == null || newContent.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Optional<ProjectComment> updatedComment = commentService.updateComment(commentId, newContent, userId);
        return updatedComment.map(ResponseEntity::ok)
                            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        Long userId = getCurrentUserId();
        boolean deleted = commentService.deleteComment(commentId, userId);
        return deleted ? ResponseEntity.noContent().build() 
                      : ResponseEntity.notFound().build();
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<ProjectComment>> getCommentsByAuthor(@PathVariable Long authorId) {
        List<ProjectComment> comments = commentService.getCommentsByAuthor(authorId);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/project/{projectId}/count")
    public ResponseEntity<Map<String, Long>> getCommentCountByProject(@PathVariable Long projectId) {
        Long count = commentService.getCommentCountByProject(projectId);
        return ResponseEntity.ok(Map.of("commentCount", count, "projectId", projectId));
    }

    @GetMapping("/project/{projectId}/search")
    public ResponseEntity<Page<ProjectComment>> searchCommentsInProject(
            @PathVariable Long projectId,
            @RequestParam String keyword,
            Pageable pageable) {
        Page<ProjectComment> comments = commentService.searchCommentsInProject(projectId, keyword, pageable);
        return ResponseEntity.ok(comments);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof HeaderAuthenticationFilter.UserAuthentication) {
            HeaderAuthenticationFilter.UserAuthentication userAuth = 
                (HeaderAuthenticationFilter.UserAuthentication) authentication;
            return userAuth.getUserIdAsLong();
        }
        return 1L; // Default fallback
    }
} 