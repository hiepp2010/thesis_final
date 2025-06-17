package com.microservices.hrms.service;

import com.microservices.hrms.dto.UserInfoDto;
import com.microservices.hrms.entity.Employee;
import com.microservices.hrms.entity.EmployeePersonalInfo;
import com.microservices.hrms.repository.EmployeeRepository;
import com.microservices.hrms.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserInfoService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserInfoService.class);
    
    @Autowired
    private EmployeeRepository employeeRepository;
    
    @Autowired
    private ManagerService managerService;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    /**
     * Get public information for all users - accessible by everyone
     */
    public List<UserInfoDto.PublicInfo> getAllUsersPublicInfo() {
        List<Employee> allEmployees = employeeRepository.findAllActiveEmployees();
        
        return allEmployees.stream()
                .map(this::convertToPublicInfo)
                .collect(Collectors.toList());
    }
    
    /**
     * Get user information based on current user's permissions
     * - Regular users: public info only
     * - Managers: full info for their reports, public info for others
     * - HR: full info for everyone
     */
    public Object getUserInfo(Long targetAuthUserId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        Optional<Employee> targetEmployee = employeeRepository.findByAuthUserId(targetAuthUserId);
        if (targetEmployee.isEmpty()) {
            throw new RuntimeException("Employee not found");
        }
        
        Employee employee = targetEmployee.get();
        
        // Check access level
        AccessLevel accessLevel = determineAccessLevel(currentUserId, targetAuthUserId);
        
        switch (accessLevel) {
            case FULL_ACCESS:
                return convertToFullInfo(employee, true); // Can view salary
            case MANAGER_ACCESS:
                return convertToFullInfo(employee, false); // Cannot view salary unless HR
            case PUBLIC_ACCESS:
            default:
                return convertToPublicInfo(employee);
        }
    }
    
    /**
     * Get all users that current user can see with appropriate level of detail
     */
    public Object getAllUsersWithPermissions() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new RuntimeException("User not authenticated");
        }
        
        if (SecurityUtils.isCurrentUserHR()) {
            // HR sees full info for everyone
            List<Employee> allEmployees = employeeRepository.findAllActiveEmployees();
            return allEmployees.stream()
                    .map(emp -> convertToFullInfo(emp, true)) // HR can see salary
                    .collect(Collectors.toList());
        } else if (SecurityUtils.isCurrentUserAnyManager()) {
            // Managers see full info for their reports, public info for others
            List<Employee> managedEmployees = managerService.getAllManagedEmployees(currentUserId);
            List<Employee> allEmployees = employeeRepository.findAllActiveEmployees();
            
            return allEmployees.stream()
                    .map(emp -> {
                        boolean canManage = managedEmployees.stream()
                                .anyMatch(managed -> managed.getAuthUserId().equals(emp.getAuthUserId()));
                        
                        if (canManage) {
                            return convertToFullInfo(emp, false); // Managers can't see salary
                        } else {
                            return convertToPublicInfo(emp);
                        }
                    })
                    .collect(Collectors.toList());
        } else {
            // Regular users see public info for everyone
            return getAllUsersPublicInfo();
        }
    }
    
    private AccessLevel determineAccessLevel(Long currentUserId, Long targetAuthUserId) {
        // Current user viewing their own profile gets full access (except salary)
        if (currentUserId.equals(targetAuthUserId)) {
            return AccessLevel.MANAGER_ACCESS;
        }
        
        // HR has full access to everyone
        if (SecurityUtils.isCurrentUserHR()) {
            return AccessLevel.FULL_ACCESS;
        }
        
        // Check if current user is a manager of the target user
        if (SecurityUtils.isCurrentUserAnyManager()) {
            boolean canManage = managerService.canManagerManageEmployee(currentUserId, targetAuthUserId);
            if (canManage) {
                return AccessLevel.MANAGER_ACCESS;
            }
        }
        
        // Default to public access
        return AccessLevel.PUBLIC_ACCESS;
    }
    
    private UserInfoDto.PublicInfo convertToPublicInfo(Employee employee) {
        return UserInfoDto.PublicInfo.builder()
                .authUserId(employee.getAuthUserId())
                .employeeId(employee.getEmployeeId())
                .name(employee.getName())
                .email(employee.getEmail())
                .department(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .jobPosition(employee.getJobPosition() != null ? employee.getJobPosition().getTitle() : null)
                .avatarUrl(getAvatarUrl(employee.getProfilePictureUrl()))
                .employmentType(employee.getEmploymentType())
                .workLocation(employee.getWorkLocation())
                .status(employee.getStatus())
                .skills(employee.getSkills())
                .bio(employee.getBio())
                .build();
    }
    
    private UserInfoDto.FullInfo convertToFullInfo(Employee employee, boolean canViewSalary) {
        UserInfoDto.FullInfo.FullInfoBuilder builder = UserInfoDto.FullInfo.builder()
                .authUserId(employee.getAuthUserId())
                .employeeId(employee.getEmployeeId())
                .name(employee.getName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .department(employee.getDepartment() != null ? employee.getDepartment().getName() : null)
                .jobPosition(employee.getJobPosition() != null ? employee.getJobPosition().getTitle() : null)
                .managerName(employee.getManager() != null ? employee.getManager().getName() : null)
                .hireDate(employee.getHireDate())
                .employmentType(employee.getEmploymentType())
                .workLocation(employee.getWorkLocation())
                .status(employee.getStatus())
                .avatarUrl(getAvatarUrl(employee.getProfilePictureUrl()))
                .skills(employee.getSkills())
                .bio(employee.getBio())
                .terminationDate(employee.getTerminationDate())
                .terminationReason(employee.getTerminationReason())
                .canViewSalary(canViewSalary);
        
        // Add personal info if available
        if (employee.getPersonalInfo() != null) {
            builder.personalInfo(convertToPersonalInfoSummary(employee.getPersonalInfo()));
        }
        
        // Add attendance summary
        builder.attendanceSummary(calculateAttendanceSummary(employee.getId()));
        
        // Add salary info if allowed
        if (canViewSalary && employee.getSalary() != null) {
            builder.salaryInfo(UserInfoDto.SalaryInfo.builder()
                    .salary(formatSalary(employee.getSalary()))
                    .currency(employee.getCurrency())
                    .build());
        }
        
        return builder.build();
    }
    
    private UserInfoDto.PersonalInfoSummary convertToPersonalInfoSummary(EmployeePersonalInfo personalInfo) {
        // Format address from JSON map
        String formattedAddress = formatAddress(personalInfo.getCurrentAddress());
        
        return UserInfoDto.PersonalInfoSummary.builder()
                .dateOfBirth(personalInfo.getDateOfBirth())
                .nationality(personalInfo.getNationality())
                .address(formattedAddress)
                .emergencyContactName(personalInfo.getEmergencyContactName())
                .emergencyContactPhone(personalInfo.getEmergencyContactPhone())
                .build();
    }
    
    private String formatAddress(Map<String, String> addressMap) {
        if (addressMap == null || addressMap.isEmpty()) {
            return null;
        }
        
        StringBuilder address = new StringBuilder();
        if (addressMap.get("street") != null) {
            address.append(addressMap.get("street"));
        }
        if (addressMap.get("city") != null) {
            if (address.length() > 0) address.append(", ");
            address.append(addressMap.get("city"));
        }
        if (addressMap.get("state") != null) {
            if (address.length() > 0) address.append(", ");
            address.append(addressMap.get("state"));
        }
        if (addressMap.get("zipCode") != null) {
            if (address.length() > 0) address.append(" ");
            address.append(addressMap.get("zipCode"));
        }
        if (addressMap.get("country") != null) {
            if (address.length() > 0) address.append(", ");
            address.append(addressMap.get("country"));
        }
        
        return address.length() > 0 ? address.toString() : null;
    }
    
    private UserInfoDto.AttendanceSummary calculateAttendanceSummary(Long employeeId) {
        // Get current month date range
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth());
        
        // Calculate working days in current month (excluding weekends)
        int totalWorkingDays = calculateWorkingDays(startOfMonth, endOfMonth);
        
        // This would typically query the attendance repository
        // For now, returning mock data
        int presentDays = Math.min(totalWorkingDays, (int) (Math.random() * totalWorkingDays));
        double attendancePercentage = totalWorkingDays > 0 ? 
            (double) presentDays / totalWorkingDays * 100 : 0;
        
        return UserInfoDto.AttendanceSummary.builder()
                .presentDaysThisMonth(presentDays)
                .totalWorkingDaysThisMonth(totalWorkingDays)
                .attendancePercentage(Math.round(attendancePercentage * 100.0) / 100.0)
                .build();
    }
    
    private int calculateWorkingDays(LocalDate start, LocalDate end) {
        int workingDays = 0;
        LocalDate current = start;
        
        while (!current.isAfter(end)) {
            if (current.getDayOfWeek().getValue() <= 5) { // Monday to Friday
                workingDays++;
            }
            current = current.plusDays(1);
        }
        
        return workingDays;
    }
    
    private String formatSalary(BigDecimal salary) {
        if (salary == null) {
            return null;
        }
        return String.format("%,.2f", salary);
    }
    
    private String getAvatarUrl(String profilePictureUrl) {
        if (profilePictureUrl == null || profilePictureUrl.isEmpty()) {
            return null;
        }
        
        try {
            return fileStorageService.getAvatarUrl(profilePictureUrl);
        } catch (Exception e) {
            logger.warn("Failed to generate avatar URL for: {}", profilePictureUrl);
            return null;
        }
    }
    
    private enum AccessLevel {
        PUBLIC_ACCESS,     // Can see public info only
        MANAGER_ACCESS,    // Can see full info except salary
        FULL_ACCESS        // Can see everything including salary
    }
} 