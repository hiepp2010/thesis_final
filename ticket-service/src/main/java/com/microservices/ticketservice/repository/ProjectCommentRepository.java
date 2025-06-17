package com.microservices.ticketservice.repository;

import com.microservices.ticketservice.entity.ProjectComment;
import com.microservices.ticketservice.entity.Project;
import com.microservices.ticketservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectCommentRepository extends JpaRepository<ProjectComment, Long> {
    
    List<ProjectComment> findByProjectOrderByCreatedAtDesc(Project project);
    
    Page<ProjectComment> findByProjectOrderByCreatedAtDesc(Project project, Pageable pageable);
    
    List<ProjectComment> findByAuthor(User author);
    
    Page<ProjectComment> findByAuthor(User author, Pageable pageable);
    
    @Query("SELECT pc FROM ProjectComment pc WHERE pc.project = :project AND pc.author = :author ORDER BY pc.createdAt DESC")
    List<ProjectComment> findByProjectAndAuthor(@Param("project") Project project, @Param("author") User author);
    
    @Query("SELECT COUNT(pc) FROM ProjectComment pc WHERE pc.project = :project")
    Long countByProject(@Param("project") Project project);
    
    @Query("SELECT pc FROM ProjectComment pc WHERE pc.content LIKE %:keyword% AND pc.project = :project ORDER BY pc.createdAt DESC")
    Page<ProjectComment> findByProjectAndContentContaining(@Param("project") Project project, @Param("keyword") String keyword, Pageable pageable);
} 