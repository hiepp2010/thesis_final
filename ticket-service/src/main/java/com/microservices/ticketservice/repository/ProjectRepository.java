package com.microservices.ticketservice.repository;

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
public interface ProjectRepository extends JpaRepository<Project, Long> {
    
    List<Project> findByOwner(User owner);
    
    Page<Project> findByOwner(User owner, Pageable pageable);
    
    @Query("SELECT p FROM Project p WHERE :user MEMBER OF p.members")
    List<Project> findByMembersContaining(@Param("user") User user);
    
    @Query("SELECT p FROM Project p WHERE :user MEMBER OF p.members")
    Page<Project> findByMembersContaining(@Param("user") User user, Pageable pageable);
    
    @Query("SELECT p FROM Project p WHERE p.owner = :user OR :user MEMBER OF p.members")
    List<Project> findByUserInvolvement(@Param("user") User user);
    
    @Query("SELECT p FROM Project p WHERE p.owner = :user OR :user MEMBER OF p.members")
    Page<Project> findByUserInvolvement(@Param("user") User user, Pageable pageable);
    
    @Query("SELECT p FROM Project p WHERE p.name LIKE %:keyword% OR p.description LIKE %:keyword%")
    Page<Project> findByKeyword(@Param("keyword") String keyword, Pageable pageable);
} 