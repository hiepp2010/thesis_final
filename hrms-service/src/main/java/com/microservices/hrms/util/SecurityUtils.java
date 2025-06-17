package com.microservices.hrms.util;

import com.microservices.hrms.config.UserPrincipal;
import com.microservices.hrms.entity.Department;
import com.microservices.hrms.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityUtils {
    
    private static DepartmentService departmentService;
    
    @Autowired
    public void setDepartmentService(DepartmentService departmentService) {
        SecurityUtils.departmentService = departmentService;
    }
    
    public static UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getDetails() instanceof UserPrincipal) {
            return (UserPrincipal) authentication.getDetails();
        }
        return null;
    }
    
    public static Long getCurrentUserId() {
        UserPrincipal user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }
    
    public static String getCurrentUsername() {
        UserPrincipal user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }
    
    public static boolean isCurrentUserHR() {
        UserPrincipal user = getCurrentUser();
        return user != null && user.isHR();
    }
    
    public static boolean isCurrentUserEmployee() {
        UserPrincipal user = getCurrentUser();
        return user != null && user.isEmployee();
    }
    
    // === MANAGER DETECTION METHODS ===
    
    /**
     * Check if current user has MANAGER role in JWT token
     */
    public static boolean isCurrentUserManager() {
        UserPrincipal user = getCurrentUser();
        return user != null && user.isManager();
    }
    
    /**
     * Check if current user is a department head (position-based manager)
     */
    public static boolean isCurrentUserDepartmentHead() {
        Long userId = getCurrentUserId();
        if (userId == null || departmentService == null) {
            return false;
        }
        return departmentService.isAuthUserDepartmentHead(userId);
    }
    
    /**
     * Check if current user is ANY kind of manager (role-based OR position-based)
     */
    public static boolean isCurrentUserAnyManager() {
        return isCurrentUserManager() || isCurrentUserDepartmentHead();
    }
    
    /**
     * Get the department managed by current user (if they are a department head)
     */
    public static Optional<Department> getCurrentUserManagedDepartment() {
        Long userId = getCurrentUserId();
        if (userId == null || departmentService == null) {
            return Optional.empty();
        }
        return departmentService.getDepartmentManagedByAuthUser(userId);
    }
    
    /**
     * Check if current user can manage a specific department
     */
    public static boolean canCurrentUserManageDepartment(Long departmentId) {
        if (isCurrentUserHR()) {
            return true; // HR can manage any department
        }
        
        Optional<Department> managedDept = getCurrentUserManagedDepartment();
        return managedDept.isPresent() && managedDept.get().getId().equals(departmentId);
    }
    
    /**
     * Get all roles of current user
     */
    public static String getCurrentUserRoles() {
        UserPrincipal user = getCurrentUser();
        return user != null ? user.getRoles() : null;
    }
} 