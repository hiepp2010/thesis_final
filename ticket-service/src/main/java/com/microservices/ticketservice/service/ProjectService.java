package com.microservices.ticketservice.service;

import com.microservices.ticketservice.entity.Project;
import com.microservices.ticketservice.entity.User;
import com.microservices.ticketservice.repository.ProjectRepository;
import com.microservices.ticketservice.repository.UserRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    // Cache project by ID (frequently accessed for permission checks)
    @Cacheable(value = "projects", key = "#projectId")
    public Optional<Project> getProjectById(Long projectId) {
        return projectRepository.findById(projectId);
    }

    // Cache user's projects list
    @Cacheable(value = "user-projects", key = "#userId")
    public List<Project> getProjectsByOwner(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.map(projectRepository::findByOwner).orElse(new ArrayList<>());
    }

    // Cache project members (critical for permission checks)
    @Cacheable(value = "project-members", key = "#projectId")
    public List<User> getProjectMembers(Long projectId) {
        Optional<Project> project = getProjectById(projectId);
        return project.map(Project::getMembers).orElse(new ArrayList<>());
    }

    public List<Project> getProjectsByMember(Long memberId) {
        Optional<User> user = userRepository.findById(memberId);
        return user.map(projectRepository::findByMembersContaining).orElse(List.of());
    }

    @CacheEvict(value = "user-projects", key = "#ownerId")
    public Project createProject(ProjectCreateDto projectDto, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + ownerId));

        Project project = Project.builder()
                .name(projectDto.getName())
                .description(projectDto.getDescription())
                .owner(owner)
                .build();

        // Add members if provided
        if (projectDto.getMemberIds() != null && !projectDto.getMemberIds().isEmpty()) {
            List<User> members = userRepository.findAllById(projectDto.getMemberIds());
            project.setMembers(members);
        }

        return projectRepository.save(project);
    }

    public Optional<Project> updateProject(Long id, ProjectUpdateDto projectDto, Long currentUserId) {
        return projectRepository.findById(id).map(project -> {
            // Only project owner can update project
            if (!project.getOwner().getId().equals(currentUserId)) {
                throw new RuntimeException("Access denied. Only project owner can update the project.");
            }

            if (projectDto.getName() != null) {
                project.setName(projectDto.getName());
            }
            if (projectDto.getDescription() != null) {
                project.setDescription(projectDto.getDescription());
            }
            if (projectDto.getMemberIds() != null) {
                List<User> members = userRepository.findAllById(projectDto.getMemberIds());
                project.setMembers(members);
            }

            return projectRepository.save(project);
        });
    }

    public boolean deleteProject(Long id, Long currentUserId) {
        return projectRepository.findById(id).map(project -> {
            // Only project owner can delete project
            if (!project.getOwner().getId().equals(currentUserId)) {
                throw new RuntimeException("Access denied. Only project owner can delete the project.");
            }

            projectRepository.delete(project);
            return true;
        }).orElse(false);
    }

    public Optional<Project> addMemberToProject(Long projectId, Long memberId, Long currentUserId) {
        return projectRepository.findById(projectId).map(project -> {
            // Only project owner can add members
            if (!project.getOwner().getId().equals(currentUserId)) {
                throw new RuntimeException("Access denied. Only project owner can add members to the project.");
            }

            User member = userRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + memberId));
            
            if (!project.getMembers().contains(member)) {
                project.getMembers().add(member);
                return projectRepository.save(project);
            }
            return project;
        });
    }

    public Optional<Project> removeMemberFromProject(Long projectId, Long memberId, Long currentUserId) {
        return projectRepository.findById(projectId).map(project -> {
            // Only project owner can remove members
            if (!project.getOwner().getId().equals(currentUserId)) {
                throw new RuntimeException("Access denied. Only project owner can remove members from the project.");
            }

            User member = userRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + memberId));
            
            // Prevent owner from removing themselves
            if (member.getId().equals(project.getOwner().getId())) {
                throw new RuntimeException("Project owner cannot be removed from the project. Transfer ownership first.");
            }

            project.getMembers().remove(member);
            return projectRepository.save(project);
        });
    }

    // Cache membership check (most frequent operation)
    @Cacheable(value = "project-members", key = "#projectId + '-member-' + #userId")
    public boolean isUserProjectMember(Long userId, Long projectId) {
        try {
            Optional<Project> project = projectRepository.findById(projectId);
            if (project.isEmpty()) {
                return false;
            }

            Project proj = project.get();
            
            // Check if user is the project owner (this should always work)
            if (proj.getOwner() != null && proj.getOwner().getId().equals(userId)) {
                return true;
            }
            
            // Try to check members list, but catch any lazy loading exceptions
            try {
                if (proj.getMembers() != null) {
                    return proj.getMembers().stream()
                            .anyMatch(member -> member != null && member.getId().equals(userId));
                }
            } catch (Exception e) {
                // If there's a lazy loading exception, just return false for member check
                // but the user might still be the owner (checked above)
                System.out.println("Warning: Could not check project members due to: " + e.getMessage());
            }
            
            return false;
        } catch (Exception e) {
            System.out.println("Error in isUserProjectMember: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Check if user is project owner
     */
    public boolean isUserProjectOwner(Long userId, Long projectId) {
        Optional<Project> project = projectRepository.findById(projectId);
        return project.map(proj -> proj.getOwner().getId().equals(userId)).orElse(false);
    }

    @Data
    public static class ProjectCreateDto {
        private String name;
        private String description;
        private List<Long> memberIds;
    }

    @Data
    public static class ProjectUpdateDto {
        private String name;
        private String description;
        private List<Long> memberIds;
    }
} 