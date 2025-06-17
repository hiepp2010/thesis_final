package com.microservices.hrms.controller;

import com.microservices.hrms.entity.Employee;
import com.microservices.hrms.service.ManagerService;
import com.microservices.hrms.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hrms/managers")
public class ManagerController {
    
    @Autowired
    private ManagerService managerService;
    
    // Get all employees managed by current user
    @GetMapping("/my-team")
    public ResponseEntity<Map<String, Object>> getMyTeam() {
        Map<String, Object> response = new HashMap<>();
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }
        
        // Check if user is any kind of manager
        if (!SecurityUtils.isCurrentUserAnyManager() && !SecurityUtils.isCurrentUserHR()) {
            response.put("error", "Access denied. Only managers can view team members.");
            response.put("isManager", false);
            return ResponseEntity.status(403).body(response);
        }
        
        try {
            ManagerService.ManagerSummary summary = managerService.getManagerSummary(currentUserId);
            
            response.put("manager", summary.getManagerEmployee());
            response.put("managedDepartment", summary.getManagedDepartment());
            response.put("directReports", summary.getDirectReports());
            response.put("departmentMembers", summary.getDepartmentMembers());
            response.put("allManagedEmployees", summary.getAllManagedEmployees());
            response.put("summary", Map.of(
                "totalManagedEmployees", summary.getTotalManagedCount(),
                "directReportsCount", summary.getDirectReportsCount(),
                "departmentMembersCount", summary.getDepartmentMembersCount(),
                "isDepartmentHead", summary.isDepartmentHead(),
                "hasDirectReports", summary.isHasDirectReports()
            ));
            
            // Management type analysis
            String managementType = "Unknown";
            if (SecurityUtils.isCurrentUserHR()) {
                managementType = "HR Manager (can manage all employees)";
            } else if (summary.isDepartmentHead() && summary.isHasDirectReports()) {
                managementType = "Department Head + Direct Manager";
            } else if (summary.isDepartmentHead()) {
                managementType = "Department Head";
            } else if (summary.isHasDirectReports()) {
                managementType = "Direct Manager";
            }
            
            response.put("managementType", managementType);
            response.put("isManager", true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Get organizational chart for current manager
    @GetMapping("/org-chart")
    public ResponseEntity<Map<String, Object>> getOrganizationalChart() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }
        
        if (!SecurityUtils.isCurrentUserAnyManager() && !SecurityUtils.isCurrentUserHR()) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied. Only managers can view org chart."));
        }
        
        try {
            Map<String, Object> orgChart = managerService.getOrganizationalChart(currentUserId);
            return ResponseEntity.ok(orgChart);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // Get specific employee details (if manager can manage them)
    @GetMapping("/employee/{employeeAuthUserId}")
    public ResponseEntity<Map<String, Object>> getManagedEmployeeDetails(@PathVariable Long employeeAuthUserId) {
        Map<String, Object> response = new HashMap<>();
        
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            response.put("error", "User not authenticated");
            return ResponseEntity.status(401).body(response);
        }
        
        // Check if current user can manage this employee
        boolean canManage = SecurityUtils.isCurrentUserHR() || 
                           managerService.canManagerManageEmployee(currentUserId, employeeAuthUserId);
        
        if (!canManage) {
            response.put("error", "Access denied. You cannot manage this employee.");
            response.put("canManage", false);
            return ResponseEntity.status(403).body(response);
        }
        
        try {
            // Get the employee details
            // Note: You would implement this method in EmployeeService
            response.put("canManage", true);
            response.put("message", "Employee access granted");
            response.put("employeeAuthUserId", employeeAuthUserId);
            response.put("managerAuthUserId", currentUserId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Assign direct manager to an employee (HR only)
    @PostMapping("/assign-direct-manager")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> assignDirectManager(
            @RequestParam Long employeeId,
            @RequestParam Long managerAuthUserId) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Employee updatedEmployee = managerService.assignDirectManager(employeeId, managerAuthUserId);
            
            response.put("success", true);
            response.put("message", "Direct manager assigned successfully");
            response.put("employee", updatedEmployee);
            response.put("manager", updatedEmployee.getManager());
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    // Remove direct manager from an employee (HR only)
    @DeleteMapping("/remove-direct-manager/{employeeId}")
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> removeDirectManager(@PathVariable Long employeeId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Employee updatedEmployee = managerService.removeDirectManager(employeeId);
            
            response.put("success", true);
            response.put("message", "Direct manager removed successfully");
            response.put("employee", updatedEmployee);
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 