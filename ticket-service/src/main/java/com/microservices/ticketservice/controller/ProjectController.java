package com.microservices.ticketservice.controller;

import com.microservices.ticketservice.config.HeaderAuthenticationFilter;
import com.microservices.ticketservice.entity.Project;
import com.microservices.ticketservice.service.ProjectService;
import com.microservices.ticketservice.service.ProjectService.ProjectCreateDto;
import com.microservices.ticketservice.service.ProjectService.ProjectUpdateDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/projects")
@CrossOrigin(origins = "*")
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<Project>> getAllProjects() {
        List<Project> projects = projectService.getAllProjects();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getProjectById(@PathVariable Long id) {
        Optional<Project> project = projectService.getProjectById(id);
        return project.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Project> createProject(@Valid @RequestBody ProjectCreateDto projectDto) {
        Long userId = getCurrentUserId();
        Project createdProject = projectService.createProject(projectDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProject);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Project> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectUpdateDto projectDto) {
        
        Long currentUserId = getCurrentUserId();
        try {
            Optional<Project> updatedProject = projectService.updateProject(id, projectDto, currentUserId);
            return updatedProject.map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        try {
            boolean deleted = projectService.deleteProject(id, currentUserId);
            return deleted ? ResponseEntity.noContent().build() 
                          : ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PutMapping("/{projectId}/members/{memberId}")
    public ResponseEntity<Project> addMemberToProject(
            @PathVariable Long projectId,
            @PathVariable Long memberId) {
        
        Long currentUserId = getCurrentUserId();
        try {
            Optional<Project> updatedProject = projectService.addMemberToProject(projectId, memberId, currentUserId);
            return updatedProject.map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @DeleteMapping("/{projectId}/members/{memberId}")
    public ResponseEntity<Project> removeMemberFromProject(
            @PathVariable Long projectId,
            @PathVariable Long memberId) {
        
        Long currentUserId = getCurrentUserId();
        try {
            Optional<Project> updatedProject = projectService.removeMemberFromProject(projectId, memberId, currentUserId);
            return updatedProject.map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/owner/{ownerId}")
    public ResponseEntity<List<Project>> getProjectsByOwner(@PathVariable Long ownerId) {
        List<Project> projects = projectService.getProjectsByOwner(ownerId);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<Project>> getProjectsByMember(@PathVariable Long memberId) {
        List<Project> projects = projectService.getProjectsByMember(memberId);
        return ResponseEntity.ok(projects);
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