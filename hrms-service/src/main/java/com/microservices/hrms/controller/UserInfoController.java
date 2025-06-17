package com.microservices.hrms.controller;

import com.microservices.hrms.dto.UserInfoDto;
import com.microservices.hrms.entity.Employee;
import com.microservices.hrms.repository.EmployeeRepository;
import com.microservices.hrms.service.FileStorageService;
import com.microservices.hrms.service.UserInfoService;
import com.microservices.hrms.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/hrms/users")
public class UserInfoController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserInfoController.class);
    
    @Autowired
    private UserInfoService userInfoService;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    /**
     * Get all users with appropriate level of detail based on current user's permissions
     * - Regular users: public info for all users
     * - Managers: full info for their reports, public info for others
     * - HR: full info for everyone
     */
    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllUsers() {
        try {
            Object result = userInfoService.getAllUsersWithPermissions();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error fetching all users", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch users: " + e.getMessage()));
        }
    }
    
    /**
     * Get public information for all users - accessible by everyone
     */
    @GetMapping("/public")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllUsersPublicInfo() {
        try {
            return ResponseEntity.ok(userInfoService.getAllUsersPublicInfo());
        } catch (Exception e) {
            logger.error("Error fetching public user info", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch public user info: " + e.getMessage()));
        }
    }
    
    /**
     * Get specific user information based on current user's permissions
     */
    @GetMapping("/{authUserId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserInfo(@PathVariable Long authUserId) {
        try {
            Object result = userInfoService.getUserInfo(authUserId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error fetching user info for authUserId: {}", authUserId, e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch user info: " + e.getMessage()));
        }
    }
    
    /**
     * Get current user's own information (always full access except salary for non-HR)
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyInfo() {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User not authenticated"));
            }
            
            Object result = userInfoService.getUserInfo(currentUserId);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Error fetching current user info", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to fetch user info: " + e.getMessage()));
        }
    }
    
    /**
     * Upload avatar for current user
     */
    @PostMapping("/me/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User not authenticated"));
            }
            
            Optional<Employee> employee = employeeRepository.findByAuthUserId(currentUserId);
            if (employee.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Employee record not found"));
            }
            
            Employee emp = employee.get();
            
            // Delete old avatar if exists
            if (emp.getProfilePictureUrl() != null && !emp.getProfilePictureUrl().isEmpty()) {
                try {
                    fileStorageService.deleteAvatar(emp.getProfilePictureUrl());
                } catch (Exception e) {
                    logger.warn("Failed to delete old avatar: {}", emp.getProfilePictureUrl());
                }
            }
            
            // Upload new avatar
            String fileName = fileStorageService.uploadAvatar(emp.getId(), file);
            
            // Update employee record
            emp.setProfilePictureUrl(fileName);
            employeeRepository.save(emp);
            
            // Get avatar URL for response
            String avatarUrl = fileStorageService.getAvatarUrl(fileName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Avatar uploaded successfully");
            response.put("fileName", fileName);
            response.put("avatarUrl", avatarUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid file upload: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error uploading avatar", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to upload avatar: " + e.getMessage()));
        }
    }
    
    /**
     * Upload avatar for a specific employee (HR only)
     */
    @PostMapping("/{authUserId}/avatar")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<?> uploadAvatarForEmployee(@PathVariable Long authUserId, 
                                                    @RequestParam("file") MultipartFile file) {
        try {
            Optional<Employee> employee = employeeRepository.findByAuthUserId(authUserId);
            if (employee.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Employee record not found"));
            }
            
            Employee emp = employee.get();
            
            // Delete old avatar if exists
            if (emp.getProfilePictureUrl() != null && !emp.getProfilePictureUrl().isEmpty()) {
                try {
                    fileStorageService.deleteAvatar(emp.getProfilePictureUrl());
                } catch (Exception e) {
                    logger.warn("Failed to delete old avatar: {}", emp.getProfilePictureUrl());
                }
            }
            
            // Upload new avatar
            String fileName = fileStorageService.uploadAvatar(emp.getId(), file);
            
            // Update employee record
            emp.setProfilePictureUrl(fileName);
            employeeRepository.save(emp);
            
            // Get avatar URL for response
            String avatarUrl = fileStorageService.getAvatarUrl(fileName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Avatar uploaded successfully for employee " + emp.getName());
            response.put("fileName", fileName);
            response.put("avatarUrl", avatarUrl);
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid file upload: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error uploading avatar for authUserId: {}", authUserId, e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to upload avatar: " + e.getMessage()));
        }
    }
    
    /**
     * Delete current user's avatar
     */
    @DeleteMapping("/me/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteMyAvatar() {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User not authenticated"));
            }
            
            Optional<Employee> employee = employeeRepository.findByAuthUserId(currentUserId);
            if (employee.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Employee record not found"));
            }
            
            Employee emp = employee.get();
            
            if (emp.getProfilePictureUrl() != null && !emp.getProfilePictureUrl().isEmpty()) {
                fileStorageService.deleteAvatar(emp.getProfilePictureUrl());
                emp.setProfilePictureUrl(null);
                employeeRepository.save(emp);
            }
            
            return ResponseEntity.ok(Map.of("message", "Avatar deleted successfully"));
            
        } catch (Exception e) {
            logger.error("Error deleting avatar", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to delete avatar: " + e.getMessage()));
        }
    }
    
    /**
     * Get access summary for current user (for debugging/UI purposes)
     */
    @GetMapping("/access-info")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAccessInfo() {
        try {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "User not authenticated"));
            }
            
            Map<String, Object> accessInfo = new HashMap<>();
            accessInfo.put("userId", currentUserId);
            accessInfo.put("isHR", SecurityUtils.isCurrentUserHR());
            accessInfo.put("isManager", SecurityUtils.isCurrentUserManager());
            accessInfo.put("isDepartmentHead", SecurityUtils.isCurrentUserDepartmentHead());
            accessInfo.put("isAnyManager", SecurityUtils.isCurrentUserAnyManager());
            accessInfo.put("roles", SecurityUtils.getCurrentUserRoles());
            
            if (SecurityUtils.isCurrentUserAnyManager()) {
                accessInfo.put("managedDepartment", SecurityUtils.getCurrentUserManagedDepartment());
            }
            
            return ResponseEntity.ok(accessInfo);
            
        } catch (Exception e) {
            logger.error("Error getting access info", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get access info: " + e.getMessage()));
        }
    }
} 