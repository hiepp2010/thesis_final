package com.microservices.hrms.repository;

import com.microservices.hrms.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    
    // Find active departments
    List<Department> findByIsActiveTrue();
    
    // Find department by name
    Optional<Department> findByNameAndIsActiveTrue(String name);
    
    // Find departments by head auth user ID
    Optional<Department> findByHeadAuthUserId(Long headAuthUserId);
    
    // Check if auth user is a department head
    @Query("SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM Department d WHERE d.headAuthUserId = :authUserId AND d.isActive = true")
    boolean isAuthUserDepartmentHead(@Param("authUserId") Long authUserId);
    
    // Get departments with their head employee details
    @Query("SELECT d FROM Department d LEFT JOIN Employee e ON d.headAuthUserId = e.authUserId WHERE d.isActive = true")
    List<Department> findAllActiveDepartmentsWithHeads();
    
    // Find departments without heads
    @Query("SELECT d FROM Department d WHERE d.headAuthUserId IS NULL AND d.isActive = true")
    List<Department> findDepartmentsWithoutHeads();
    
    // Count employees in department
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.department.id = :departmentId AND e.status = 'ACTIVE'")
    Long countActiveEmployeesInDepartment(@Param("departmentId") Long departmentId);
} 