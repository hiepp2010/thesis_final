package com.microservices.ticketservice.service;

import com.microservices.ticketservice.dto.ProjectCommentCreateDto;
import com.microservices.ticketservice.entity.ProjectComment;
import com.microservices.ticketservice.entity.Project;
import com.microservices.ticketservice.entity.User;
import com.microservices.ticketservice.repository.ProjectCommentRepository;
import com.microservices.ticketservice.repository.ProjectRepository;
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
public class ProjectCommentService {

    @Autowired
    private ProjectCommentRepository commentRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    public List<ProjectComment> getCommentsByProject(Long projectId) {
        Optional<Project> project = projectRepository.findById(projectId);
        return project.map(commentRepository::findByProjectOrderByCreatedAtDesc).orElse(List.of());
    }

    public Page<ProjectComment> getCommentsByProject(Long projectId, Pageable pageable) {
        Optional<Project> project = projectRepository.findById(projectId);
        return project.map(p -> commentRepository.findByProjectOrderByCreatedAtDesc(p, pageable))
                     .orElse(Page.empty());
    }

    public ProjectComment createComment(ProjectCommentCreateDto commentDto, Long authorId) {
        // Get project
        Project project = projectRepository.findById(commentDto.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + commentDto.getProjectId()));

        // Get author
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + authorId));

        // Allow any authenticated user to comment on projects/tickets as requested
        // Note: Access control removed to allow any user to comment as per user requirements

        ProjectComment comment = ProjectComment.builder()
                .content(commentDto.getContent())
                .project(project)
                .author(author)
                .build();

        return commentRepository.save(comment);
    }

    public Optional<ProjectComment> updateComment(Long commentId, String newContent, Long userId) {
        return commentRepository.findById(commentId).map(comment -> {
            // Check if user is the author of the comment
            if (!comment.getAuthor().getId().equals(userId)) {
                throw new RuntimeException("User can only edit their own comments");
            }

            comment.setContent(newContent);
            return commentRepository.save(comment);
        });
    }

    public boolean deleteComment(Long commentId, Long userId) {
        return commentRepository.findById(commentId).map(comment -> {
            // Check if user is the author or project owner
            if (!comment.getAuthor().getId().equals(userId) && 
                !comment.getProject().getOwner().getId().equals(userId)) {
                throw new RuntimeException("User can only delete their own comments or project owner can delete any comment");
            }

            commentRepository.delete(comment);
            return true;
        }).orElse(false);
    }

    public List<ProjectComment> getCommentsByAuthor(Long authorId) {
        Optional<User> author = userRepository.findById(authorId);
        return author.map(commentRepository::findByAuthor).orElse(List.of());
    }

    public Long getCommentCountByProject(Long projectId) {
        Optional<Project> project = projectRepository.findById(projectId);
        return project.map(commentRepository::countByProject).orElse(0L);
    }

    public Page<ProjectComment> searchCommentsInProject(Long projectId, String keyword, Pageable pageable) {
        Optional<Project> project = projectRepository.findById(projectId);
        return project.map(p -> commentRepository.findByProjectAndContentContaining(p, keyword, pageable))
                     .orElse(Page.empty());
    }

    private boolean isUserProjectMember(User user, Project project) {
        // User is member if they are the owner or in the members list
        return project.getOwner().getId().equals(user.getId()) || 
               (project.getMembers() != null && project.getMembers().contains(user));
    }
} 