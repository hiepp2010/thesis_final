package com.microservices.hrms.repository;

import com.microservices.hrms.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    
    Optional<Employee> findByAuthUserId(Long authUserId);
    
    Optional<Employee> findByEmployeeId(String employeeId);
    
    Optional<Employee> findByEmail(String email);
    
    List<Employee> findByDepartmentId(Long departmentId);
    
    List<Employee> findByStatus(Employee.EmployeeStatus status);
    
    List<Employee> findByManagerId(Long managerId);
    
    @Query("SELECT e FROM Employee e WHERE e.status = 'ACTIVE' ORDER BY e.name")
    List<Employee> findAllActiveEmployees();
    
    @Query("SELECT e FROM Employee e WHERE " +
           "LOWER(e.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.employeeId) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Employee> searchEmployees(@Param("searchTerm") String searchTerm);
    
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.status = 'ACTIVE'")
    long countActiveEmployees();
    
    @Query("SELECT e FROM Employee e WHERE e.department.id = :departmentId AND e.status = 'ACTIVE'")
    List<Employee> findActiveEmployeesByDepartment(@Param("departmentId") Long departmentId);
    
    @Query("SELECT e FROM Employee e WHERE e.manager.authUserId = :managerAuthUserId AND e.status = 'ACTIVE'")
    List<Employee> findDirectReportsByManagerAuthUserId(@Param("managerAuthUserId") Long managerAuthUserId);
    
    @Query("SELECT e FROM Employee e WHERE e.department.id = :departmentId AND e.status = 'ACTIVE'")
    List<Employee> findActiveEmployeesByDepartmentId(@Param("departmentId") Long departmentId);
    
    @Query("SELECT e FROM Employee e WHERE e.department.headAuthUserId = :headAuthUserId AND e.status = 'ACTIVE'")
    List<Employee> findEmployeesByDepartmentHead(@Param("headAuthUserId") Long headAuthUserId);
    
    @Query("SELECT DISTINCT e FROM Employee e WHERE " +
           "(e.manager.authUserId = :authUserId OR e.department.headAuthUserId = :authUserId) " +
           "AND e.status = 'ACTIVE'")
    List<Employee> findAllManagedEmployeesByAuthUserId(@Param("authUserId") Long authUserId);
    
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.manager.authUserId = :managerAuthUserId AND e.status = 'ACTIVE'")
    Long countDirectReportsByManagerAuthUserId(@Param("managerAuthUserId") Long managerAuthUserId);
    
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.department.headAuthUserId = :headAuthUserId AND e.status = 'ACTIVE'")
    Long countDepartmentMembersByHead(@Param("headAuthUserId") Long headAuthUserId);
} 