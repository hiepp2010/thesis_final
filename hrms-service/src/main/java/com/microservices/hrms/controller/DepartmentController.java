package com.microservices.hrms.controller;

import com.microservices.hrms.entity.Department;
import com.microservices.hrms.entity.Employee;
import com.microservices.hrms.service.DepartmentService;
import com.microservices.hrms.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/hrms/departments")
public class DepartmentController {
    
    @Autowired
    private DepartmentService departmentService;
    
    // Get all active departments
    @GetMapping
    public ResponseEntity<List<Department>> getAllDepartments() {
        List<Department> departments = departmentService.getAllActiveDepartments();
        return ResponseEntity.ok(departments);
    }
    
    // Get department by ID
    @GetMapping("/{id}")
    public ResponseEntity<Department> getDepartmentById(@PathVariable Long id) {
        Optional<Department> department = departmentService.getDepartmentById(id);
        return department.map(ResponseEntity::ok)
                        .orElse(ResponseEntity.notFound().build());
    }
    
    // Create new department (HR only)
    @PostMapping
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<Department> createDepartment(@RequestBody Department department) {
        try {
            Department createdDepartment = departmentService.createDepartment(department);
            return ResponseEntity.ok(createdDepartment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Update department (HR only)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<Department> updateDepartment(@PathVariable Long id, @RequestBody Department department) {
        try {
            Department updatedDepartment = departmentService.updateDepartment(id, department);
            return ResponseEntity.ok(updatedDepartment);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    // Assign department head (HR only)
    @PostMapping("/{departmentId}/assign-head/{authUserId}")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> assignDepartmentHead(
            @PathVariable Long departmentId, 
            @PathVariable Long authUserId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Department department = departmentService.assignDepartmentHead(departmentId, authUserId);
            Optional<Employee> head = departmentService.getDepartmentHead(departmentId);
            
            response.put("success", true);
            response.put("message", "Department head assigned successfully");
            response.put("department", department);
            response.put("head", head.orElse(null));
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Remove department head (HR only)
    @DeleteMapping("/{departmentId}/remove-head")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> removeDepartmentHead(@PathVariable Long departmentId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Department department = departmentService.removeDepartmentHead(departmentId);
            
            response.put("success", true);
            response.put("message", "Department head removed successfully");
            response.put("department", department);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Get department head
    @GetMapping("/{departmentId}/head")
    public ResponseEntity<Map<String, Object>> getDepartmentHead(@PathVariable Long departmentId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<Employee> head = departmentService.getDepartmentHead(departmentId);
            
            response.put("departmentId", departmentId);
            response.put("hasHead", head.isPresent());
            response.put("head", head.orElse(null));
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Get department statistics (HR only)
    @GetMapping("/{departmentId}/stats")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<DepartmentService.DepartmentStats> getDepartmentStats(@PathVariable Long departmentId) {
        try {
            DepartmentService.DepartmentStats stats = departmentService.getDepartmentStats(departmentId);
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // Get departments without heads (HR only)
    @GetMapping("/without-heads")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<List<Department>> getDepartmentsWithoutHeads() {
        List<Department> departments = departmentService.getDepartmentsWithoutHeads();
        return ResponseEntity.ok(departments);
    }
    
    // Check if current user is a department head
    @GetMapping("/my-department")
    public ResponseEntity<Map<String, Object>> getMyManagedDepartment() {
        Map<String, Object> response = new HashMap<>();
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }
        
        // Directly check if current auth user is a department head
        Optional<Department> managedDepartment = departmentService.getDepartmentManagedByAuthUser(currentUserId);
        
        response.put("isHead", managedDepartment.isPresent());
        response.put("department", managedDepartment.orElse(null));
        response.put("authUserId", currentUserId);
        
        return ResponseEntity.ok(response);
    }
    
    // Get current user's manager status and permissions
    @GetMapping("/my-manager-status")
    public ResponseEntity<Map<String, Object>> getMyManagerStatus() {
        Map<String, Object> response = new HashMap<>();
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }
        
        // Check different types of manager status
        boolean hasManagerRole = SecurityUtils.isCurrentUserManager();
        boolean isDepartmentHead = SecurityUtils.isCurrentUserDepartmentHead();
        boolean isAnyManager = SecurityUtils.isCurrentUserAnyManager();
        boolean isHR = SecurityUtils.isCurrentUserHR();
        
        Optional<Department> managedDept = SecurityUtils.getCurrentUserManagedDepartment();
        
        response.put("authUserId", currentUserId);
        response.put("username", SecurityUtils.getCurrentUsername());
        response.put("allRoles", SecurityUtils.getCurrentUserRoles());
        response.put("hasManagerRole", hasManagerRole);
        response.put("isDepartmentHead", isDepartmentHead);
        response.put("isAnyKindOfManager", isAnyManager);
        response.put("isHR", isHR);
        response.put("managedDepartment", managedDept.orElse(null));
        
        // Manager type classification
        String managerType = "None";
        if (isHR) {
            managerType = "HR Manager";
        } else if (hasManagerRole && isDepartmentHead) {
            managerType = "Role-based & Department Head Manager";
        } else if (hasManagerRole) {
            managerType = "Role-based Manager";
        } else if (isDepartmentHead) {
            managerType = "Department Head Manager";
        }
        
        response.put("managerType", managerType);
        
        return ResponseEntity.ok(response);
    }
    
    // Get department members (accessible by department head or HR)
    @GetMapping("/{departmentId}/members")
    public ResponseEntity<Map<String, Object>> getDepartmentMembers(@PathVariable Long departmentId) {
        Map<String, Object> response = new HashMap<>();
        
        // Check authorization - either HR role or department head
        boolean isHR = SecurityUtils.isCurrentUserHR();
        boolean isDepartmentHead = false;
        
        if (!isHR) {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId != null) {
                Optional<Department> managedDept = departmentService.getDepartmentManagedByAuthUser(currentUserId);
                isDepartmentHead = managedDept.isPresent() && managedDept.get().getId().equals(departmentId);
            }
        }
        
        if (!isHR && !isDepartmentHead) {
            response.put("error", "Access denied. Only HR or department heads can view department members.");
            return ResponseEntity.status(403).body(response);
        }
        
        try {
            Optional<Department> department = departmentService.getDepartmentById(departmentId);
            if (department.isEmpty()) {
                response.put("error", "Department not found");
                return ResponseEntity.notFound().build();
            }
            
            // Get department statistics which includes employee count
            DepartmentService.DepartmentStats stats = departmentService.getDepartmentStats(departmentId);
            
            response.put("department", department.get());
            response.put("employeeCount", stats.getEmployeeCount());
            response.put("head", stats.getHead());
            response.put("members", department.get().getEmployees());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 